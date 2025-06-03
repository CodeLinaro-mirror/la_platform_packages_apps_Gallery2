/*
 * Copyright (c) 2024 Truepic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.truepic.lensverify.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.truepic.lensverify.data.c2padata.C2PAData
import com.truepic.lensverify.data.c2padata.ManifestStore
import com.truepic.lensverify.data.c2padata.assertions.actions.C2PAActionDataActions
import com.truepic.lensverify.data.c2padata.assertions.metadata.Metadata
import java.io.ByteArrayInputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class C2PAPresenter(
    private val mimeType: String?,
    private val data: C2PAData,
    private val labels: Labels
) {

    companion object {
        private val modificationsExclude = listOf("c2pa.opened", "c2pa.produced", "c2pa.created")
        private const val inDelimiter = " in "
    }

    data class Labels(
        val descriptorCreativeWork: String,
        val descriptorOriginal: String,
        val descriptorModified: String,
        val typePhoto: String,
        val typeImage: String,
        val typeVideo: String,
        val typeAudio: String,
        val capturedLabel: String,
        val createdLabel: String,
        val capturedWithLabel: String,
        val createdWithLabel: String
    )

    enum class Type {Audio, Video, Photo, Image}

    private val localDateFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.MEDIUM,
        Locale.getDefault()
    )

    private val thumbnails: Map<String, ByteArray>? = data.thumbnailStore

    @SuppressLint("SimpleDateFormat")
    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    @SuppressLint("SimpleDateFormat")
    private val inputDateFormatMilliseconds = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ")
    @SuppressLint("SimpleDateFormat")
    private val inputDateFormatNew = SimpleDateFormat("yyyy:MM:dd' 'HH:mm:ss")

    fun getManifests(): List<ManifestStore> {
        return data.manifestStore
    }

    fun getDescriptor(): String {
        return getDescriptor(data.manifestStore?.last())
    }

    fun getDescriptor(manifestStore: ManifestStore?): String {
        var descriptor: String = labels.descriptorCreativeWork

        manifestStore?.let { manifest ->
            if (!manifest.assertions.containsAiGeneratedContent()) {
                if (manifest.assertions.c2paIngredient != null && manifest.assertions.c2paIngredient.size <= 1) {
                    if (containsMakeAndModel(getManifests().first())) {
                        descriptor = labels.descriptorModified
                    }
                } else if (containsMakeAndModel(manifest)) {
                    descriptor = labels.descriptorOriginal
                }
            }
        }

        return descriptor
    }

    fun getTypeLabel(): String {
        return when(getType()) {
            Type.Audio -> labels.typeAudio
            Type.Video -> labels.typeVideo
            Type.Photo -> labels.typePhoto
            Type.Image -> labels.typeImage
        }
    }

    fun getType(): Type {
        if (mimeType.orEmpty().contains("audio")) {
            return Type.Audio
        }

        if (mimeType.orEmpty().contains("video")) {
            return Type.Video
        }

        if (containsMakeAndModel(data.manifestStore.first())) {
            return Type.Photo
        }

        return Type.Image
    }

    fun getSignedBy(): String {
        return getSignedBy(data.manifestStore?.last())
    }

    fun getSignedBy(manifestStore: ManifestStore?): String {
        return manifestStore?.certificate?.organizationName.orEmpty()
    }

    private fun getClaimGeneratorFormatted(manifestStore: ManifestStore?): String? {
        manifestStore?.claim?.let { claim ->
            claim.claimGeneratorInfo?.first()?.let {
                return it.name + " " + it.version
            }

            claim.claimGenerator?.let {
                return it
            }
        }

        return null
    }

    fun isAiGenerated(manifestStore: ManifestStore?): Boolean {
        getManifests().forEach {
            if (it.assertions?.containsAiGeneratedContent() == true) {
                // one of the previous manifests or current one contains ai content
                // hence we mark all manifests from now on as ai generated
                return true
            }

            // stop upon reaching currently checked manifest
            if(it.uri.equals(manifestStore?.uri)) return false
        }

        return false
    }

    fun isAiGenerated(): Boolean {
        return isAiGenerated(data.manifestStore?.last())
    }

    fun getCapturedWith(manifestStore: ManifestStore?): String {
        return manifestStore?.let {
            if(it.claim.claimGeneratorInfo?.first() != null) {
                it.claim.claimGeneratorInfo!!.first().name + " " + it.claim.claimGeneratorInfo!!.first().version
            } else {
                // for example Graphics_App/1.2.3 will be parsed out as Graphics App 1.2.3
                it.claim.claimGenerator.substringBefore(" ").replace("_", " ")
                    .replace("/", " ")
            }
        }.orEmpty()
    }

    fun getCapturedWith(): String {
        return getCapturedWith(data.manifestStore?.last())
    }

    fun getCapturedWithLabel(): String {
        return getCapturedWithLabel(data.manifestStore.last())
    }

    fun getCapturedWithLabel(manifestStore: ManifestStore?): String {
        return if (containsMakeAndModel(manifestStore)) {
            labels.capturedWithLabel
        } else {
            labels.createdWithLabel
        }
    }

    fun getCapturedLabel(manifestStore: ManifestStore?): String {
        return if (containsMakeAndModel(manifestStore)) {
            labels.capturedLabel
        } else {
            labels.createdLabel
        }
    }

    fun getCapturedLabel(): String {
        return getCapturedLabel(data.manifestStore.last())
    }

    fun getCapturedDate(manifestStore: ManifestStore?): String {
        var date = ""

        try {
            manifestStore?.let { store ->

                // "c2pa.actions".data.metadata.dateTime
                store.assertions.c2paActions?.lastOrNull {
                    it.data?.metadata?.dateTime != null
                }?.let { c2paAction ->
                    date = formatDate(c2paAction.data.metadata.dateTime)
                }

                // exif:DateTimeOriginal
                if (date.isEmpty()) {
                    store.assertions.metadata?.lastOrNull {
                        it.data.dateTimeOriginal != null
                    }?.let {
                        date = formatDate(it.data.dateTimeOriginal)
                    }
                }

                // "c2pa.actions".data.actions.when
                if (date.isEmpty()) {
                    store.assertions.c2paActions?.forEach { c2PAAction ->
                        c2PAAction.data.actions.lastOrNull { it.`when` != null }?.let {
                            date = formatDate(it.`when`)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // could be ignored, returning empty date
        }

        return date
    }

    fun getCapturedDate(): String {
        return getCapturedDate(data.manifestStore?.last())
    }

    fun getModifications(manifestStore: ManifestStore?): Int {
        var modifications = 0

        // modifications are based on existing c2paActions excluding one in modificationsExclude
        manifestStore?.assertions?.c2paActions?.forEach { action ->
            action.data?.actions?.let {
                modifications += it.filter { c2PAActionDataItem: C2PAActionDataActions? ->
                    modificationsExclude.forEach { exclude ->
                        if (c2PAActionDataItem?.action.orEmpty().contains(exclude)) {
                            return@filter false
                        }
                    }
                    true
                }.size
            }
        }

        return modifications
    }

    fun getModifications(): Int {
        return getModifications(data.manifestStore?.last())
    }

    fun getThumbnail(manifestStore: ManifestStore, size: Int = 1024): Bitmap? {
        try {
            // retrieve thumbnail id from existing assertions
            val thumbnailId: String = manifestStore.assertions.let {
                it.c2paThumbnailClaimJpeg.first().thumbnailID ?:
                it.c2paThumbnailClaimPng.first().thumbnailID ?:
                it.c2paThumbnailIngredientJpeg.first().thumbnailID ?:
                it.c2paThumbnailIngredientPng.first().thumbnailID
            }

            thumbnails?.get(thumbnailId)?.let {
                val orientation: Int = try {
                    val exifInterface = ExifInterface(ByteArrayInputStream(it))
                    exifInterface.getAttributeInt("Orientation", 1)
                } catch (e: java.lang.Exception) {
                    1
                }

                return Util.getScaledBitmapFromBuffer(
                    it, size,
                    Util.getDegreesFromExifOrientation(orientation)
                )
            }
        } catch (e: Exception) {
            // could be ignored
        }

        return null
    }

    fun getLastThumbnail(): Bitmap? {
        return getThumbnail(data.manifestStore.last())
    }

    fun hasHistory(): Boolean {
        return (data.manifestStore?.size ?: 0) > 1
    }

    suspend fun getAddress(context: Context): String? {
        return getAddress(context, data.manifestStore?.last())
    }

    @Suppress("DEPRECATION")
    suspend fun getAddress(context: Context, store: ManifestStore?): String? =
        suspendCoroutine { continuation ->
            var callbackRunning = false

            store?.assertions?.metadata?.forEach {
                if (it.data?.longitude.orEmpty().isNotEmpty() && it.data?.latitude.orEmpty()
                        .isNotEmpty()
                ) {
                    val longitude = it.data.longitude.toDouble()
                    val latitude = it.data.latitude.toDouble()
                    callbackRunning = true

                    if (Geocoder.isPresent().not()) {
                        // geocoding not present, fallback to coordinates
                        continuation.resume("$latitude, $longitude")
                        return@forEach
                    }

                    // failback after 5 seconds
                    val timer = Timer("FailBack", false)
                    timer.schedule(timerTask {
                        continuation.resume("$latitude, $longitude")
                    }, 5000)

                    val geocoder = Geocoder(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(latitude, longitude, 1) { address ->
                            timer.cancel() // cancel fail back
                            continuation.resume(buildAddress(address.first()))
                        }
                    } else {
                        val address = geocoder.getFromLocation(latitude, longitude, 1)
                        timer.cancel() // cancel fail back
                        continuation.resume(buildAddress(address?.first()))
                    }

                    return@forEach
                }
            }

            if (!callbackRunning) {
                continuation.resume(null)
            }
        }

    private fun buildAddress(address: Address?): String {
        if (address == null) return ""

        val ret = StringBuilder("")

        if (address.locality.isNullOrEmpty().not()) {
            ret.append(address.locality)
        }

        if (address.adminArea.isNullOrEmpty().not()) {
            if (ret.isNotEmpty()) ret.append(", ")
            ret.append(stateAbbreviation(address.adminArea))
        }

        if (address.countryCode.isNullOrEmpty().not()) {
            if (ret.isNotEmpty()) ret.append(", ")

            if (address.locality.isNullOrEmpty().not()) {
                ret.append(address.countryCode)
            } else {
                ret.append(address.countryName)
            }
        }

        return ret.toString()
    }

    private fun stateAbbreviation(state: String): String {
        return when (state) {
            "Alabama" -> "AL"
            "Alaska" -> "AK"
            "Alberta" -> "AB"
            "American Samoa" -> "AS"
            "Arizona" -> "AZ"
            "Arkansas" -> "AR"
            "Armed Forces (AE)" -> "AE"
            "Armed Forces Americas" -> "AA"
            "Armed Forces Pacific" -> "AP"
            "British Columbia" -> "BC"
            "California" -> "CA"
            "Colorado" -> "CO"
            "Connecticut" -> "CT"
            "Delaware" -> "DE"
            "District Of Columbia" -> "DC"
            "Florida" -> "FL"
            "Georgia" -> "GA"
            "Guam" -> "GU"
            "Hawaii" -> "HI"
            "Idaho" -> "ID"
            "Illinois" -> "IL"
            "Indiana" -> "IN"
            "Iowa" -> "IA"
            "Kansas" -> "KS"
            "Kentucky" -> "KY"
            "Louisiana" -> "LA"
            "Maine" -> "ME"
            "Manitoba" -> "MB"
            "Maryland" -> "MD"
            "Massachusetts" -> "MA"
            "Michigan" -> "MI"
            "Minnesota" -> "MN"
            "Mississippi" -> "MS"
            "Missouri" -> "MO"
            "Montana" -> "MT"
            "Nebraska" -> "NE"
            "Nevada" -> "NV"
            "New Brunswick" -> "NB"
            "New Hampshire" -> "NH"
            "New Jersey" -> "NJ"
            "New Mexico" -> "NM"
            "New York" -> "NY"
            "Newfoundland" -> "NF"
            "North Carolina" -> "NC"
            "North Dakota" -> "ND"
            "Northwest Territories" -> "NT"
            "Nova Scotia" -> "NS"
            "Nunavut" -> "NU"
            "Ohio" -> "OH"
            "Oklahoma" -> "OK"
            "Ontario" -> "ON"
            "Oregon" -> "OR"
            "Pennsylvania" -> "PA"
            "Prince Edward Island" -> "PE"
            "Puerto Rico" -> "PR"
            "Quebec" -> "PQ"
            "Rhode Island" -> "RI"
            "Saskatchewan" -> "SK"
            "South Carolina" -> "SC"
            "South Dakota" -> "SD"
            "Tennessee" -> "TN"
            "Texas" -> "TX"
            "Utah" -> "UT"
            "Vermont" -> "VT"
            "Virgin Islands" -> "VI"
            "Virginia" -> "VA"
            "Washington" -> "WA"
            "West Virginia" -> "WV"
            "Wisconsin" -> "WI"
            "Wyoming" -> "WY"
            "Yukon Territory" -> "YT"
            else -> state
        }
    }

    private fun containsMakeAndModel(store: ManifestStore?): Boolean {
        store?.assertions?.metadata?.forEach { stdsExif: Metadata? ->
            if (stdsExif?.data?.make.isNullOrEmpty().not() &&
                stdsExif?.data?.model.isNullOrEmpty().not()
            ) {
                return true
            }
        }

        return false
    }

    /**
     * This method will try to parse date using three different formats we currently accept
     * into local date format based on the users settings
     */
    private fun formatDate(date: String): String {
        var retDate = ""

        try {
            inputDateFormat.parse(date.replace("Z", "+0000"))?.let {
                retDate = localDateFormat.format(it)
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            inputDateFormatMilliseconds.parse(date.replace("Z", "+0000"))?.let {
                retDate = localDateFormat.format(it)
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            inputDateFormatNew.parse(date.replace("Z", "+0000"))?.let {
                retDate = localDateFormat.format(it)
            }
        } catch (e: Exception) {
            // ignore
        }

        return retDate
    }
}