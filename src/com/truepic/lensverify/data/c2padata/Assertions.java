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
package com.truepic.lensverify.data.c2padata;

import com.google.gson.annotations.SerializedName;
import com.truepic.lensverify.data.c2padata.assertions.C2PAIngredient;
import com.truepic.lensverify.data.c2padata.assertions.CustomBlur;
import com.truepic.lensverify.data.c2padata.assertions.DataInstance;
import com.truepic.lensverify.data.c2padata.assertions.LibC2PA;
import com.truepic.lensverify.data.c2padata.assertions.ThumbnailAssertion;
import com.truepic.lensverify.data.c2padata.assertions.actions.C2PAAction;
import com.truepic.lensverify.data.c2padata.assertions.ai.CustomAI;
import com.truepic.lensverify.data.c2padata.assertions.creativework.StdsCreativeWork;
import com.truepic.lensverify.data.c2padata.assertions.exif.StdsExif;
import com.truepic.lensverify.data.c2padata.assertions.odometry.CustomOdometry;

import java.util.List;

public class Assertions {
    @SerializedName("c2pa.hash.data")
    private List<DataInstance> c2paHashData;
    @SerializedName("c2pa.thumbnail.claim.jpeg")
    private List<ThumbnailAssertion> c2paThumbnailClaimJpeg;
    @SerializedName("c2pa.thumbnail.ingredient.jpeg")
    private List<ThumbnailAssertion> c2paThumbnailIngredientJpeg;
    @SerializedName("com.truepic.custom.odometry")
    private List<CustomOdometry> truepicOdometry;
    @SerializedName("com.truepic.custom.blur")
    private List<CustomBlur> truepicBlur;
    @SerializedName("com.truepic.libc2pa")
    private List<LibC2PA> truepicLibC2PA;
    @SerializedName("stds.exif")
    private List<StdsExif> stdsExif;
    @SerializedName("com.truepic.custom.ai")
    private List<CustomAI> customAi;
    @SerializedName("c2pa.ingredient")
    private List<C2PAIngredient> c2paIngredient;
    @SerializedName("c2pa.actions")
    private List<C2PAAction> c2paActions;
    @SerializedName("stds.schema-org.CreativeWork")
    private List<StdsCreativeWork> stdsCreativeWork;

    public List<DataInstance> getC2paHashData() {
        return c2paHashData;
    }

    public List<ThumbnailAssertion> getC2paThumbnailClaimJpeg() {
        return c2paThumbnailClaimJpeg;
    }

    public List<ThumbnailAssertion> getC2paThumbnailIngredientJpeg() {
        return c2paThumbnailIngredientJpeg;
    }

    public List<CustomOdometry> getTruepicOdometry() {
        return truepicOdometry;
    }

    public List<LibC2PA> getTruepicLibC2PA() {
        return truepicLibC2PA;
    }

    public List<StdsExif> getStdsExif() {
        return stdsExif;
    }

    public List<CustomAI> getCustomAi() {
        return customAi;
    }

    public boolean containsAiGeneratedContent() {
        return customAi != null && !customAi.isEmpty();
    }

    public List<C2PAIngredient> getC2paIngredient() {
        return c2paIngredient;
    }

    public List<C2PAAction> getC2paActions() {
        return c2paActions;
    }

    public List<StdsCreativeWork> getStdsCreativeWork() {
        return stdsCreativeWork;
    }
}
