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
package com.truepic.lensverify.data.c2padata.assertions.exif;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ExifData {

    @SerializedName("@context")
    private Map<String, String> context;

    @SerializedName(value = "EXIF:GPSLongitude", alternate = {"exif:GPSLongitude", "exif:gpslongitude"})
    private String longitude;

    @SerializedName(value = "EXIF:GPSLatitude", alternate = {"exif:GPSLatitude", "exif:gpslatitude"})
    private String latitude;

    @SerializedName(value = "EXIF:Make", alternate = {"exif:Make", "exif:make", "tiff:Make"})
    private String make;

    @SerializedName(value = "EXIF:Model", alternate = {"exif:Model", "tiff:Model", "tiff:model"})
    private String model;

    @SerializedName(value = "EXIF:GPSAltitude", alternate = {"exif:GPSAltitude", "exif:gpsaltitude"})
    private String gpsAltitude;

    @SerializedName(value = "EXIF:GPSHorizontalAccuracy", alternate = {"exif:GPSHorizontalAccuracy", "exif:gpshorizontalaccuracy"})
    private String gpsAccuracy;

    @SerializedName(value = "EXIF:GPSTimeStamp", alternate = {"exif:GPSTimeStamp", "exif:gpstimestamp"})
    private String gpsTimestamp;

    @SerializedName(value = "EXIF:DateTimeOriginal", alternate = {"exif:DateTimeOriginal", "exif:datetimeoriginal"})
    private String dateTimeOriginal;

    @SerializedName("truepic_id")
    private String truepicId;

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getGPSAltitude() {
        return gpsAltitude;
    }

    public String getGPSAccuracy() {
        return gpsAccuracy;
    }

    public String getGPSTimestamp() {
        return gpsTimestamp;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public String getTruepicId() {
        return truepicId;
    }

    public String getDateTimeOriginal() {
        return dateTimeOriginal;
    }
}
