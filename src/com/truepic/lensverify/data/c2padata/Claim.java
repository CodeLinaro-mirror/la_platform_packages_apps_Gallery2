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

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Claim {

    @SerializedName("dc:title")
    private String dcTitle;

    @SerializedName("dc:format")
    private String dcFormat;

    private String instanceID;

    @SerializedName("claim_generator")
    private String claimGenerator;

    @SerializedName("claim_generator_info")
    private JsonElement claimGeneratorInfo;

    private boolean isActive;

    public String getDcTitle() {
        return dcTitle;
    }

    public String getDcFormat() {
        return dcFormat;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public String getClaimGenerator() {
        return claimGenerator;
    }

    public @Nullable List<ClaimGeneratorInfo> getClaimGeneratorInfo() {
        if (claimGeneratorInfo == null) return null;

        if (claimGeneratorInfo.isJsonArray()) { // pre 2.0 spec we have list of ClaimGeneratorInfo(s)
            Type listType = new TypeToken<List<ClaimGeneratorInfo>>(){}.getType();
            return (new Gson()).fromJson(claimGeneratorInfo, listType);
        } else { // 2.0 we have single ClaimGeneratorInfo
            ArrayList<ClaimGeneratorInfo> list = new ArrayList<>();
            list.add((new Gson()).fromJson(claimGeneratorInfo, ClaimGeneratorInfo.class));
            return list;
        }
    }

    public boolean isActive() {
        return isActive;
    }
}
