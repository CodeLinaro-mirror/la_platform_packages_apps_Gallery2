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
/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.gallery3d.c2pa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.gallery3d.R;
import com.android.gallery3d.util.C2paUtil;
import com.truepic.lensverify.data.c2padata.C2PAData;
import com.truepic.lensverify.utils.C2PAPresenter;
import com.truepic.lensverify.data.c2padata.ManifestStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class C2paActivity extends Activity {

    private static final String TAG = C2paActivity.class.getSimpleName();

    public static final String FILE_PATH = "FILE_PATH";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c2pa);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String filePath = getIntent().getStringExtra(FILE_PATH);
        C2paUtil c2paUtil = new C2paUtil(filePath);
        c2paUtil.validateImage();
        C2PAData c2PAData = c2paUtil.getImageC2paData();
        C2paUtil.C2PAStatus status = c2paUtil.getC2PAStatus(c2PAData);

        RecyclerView list = findViewById(R.id.list);
        TextView message = findViewById(R.id.message);

        if(status == C2paUtil.C2PAStatus.C2PA) {
            Resources res = getResources();
            C2PAPresenter presenter = new C2PAPresenter(c2PAData, new C2PAPresenter.Labels(
                    "",
                    "",
                    "",
                    res.getString(R.string.c2pa_info_thumbnail_type_photo),
                    res.getString(R.string.c2pa_info_thumbnail_type_image),
                    res.getString(R.string.c2pa_info_thumbnail_type_video),
                    res.getString(R.string.c2pa_info_thumbnail_type_audio),
                    res.getString(R.string.c2pa_info_captured),
                    res.getString(R.string.c2pa_info_created),
                    res.getString(R.string.c2pa_info_captured_with),
                    res.getString(R.string.c2pa_info_created_with)));

            List<ManifestStore> manifestStores = presenter.getManifests();
            Collections.reverse(manifestStores);
            ArrayList<Item> items = new ArrayList<>();
            for (ManifestStore manifestStore : manifestStores) {
                Item item = new Item(
                        getAddress((Context) C2paActivity.this, manifestStore),
                        presenter.getThumbnail(manifestStore, 200),
                        presenter.getType(),
                        presenter.getTypeLabel(),
                        presenter.getCapturedWith(manifestStore),
                        presenter.getCapturedWithLabel(manifestStore),
                        presenter.getCapturedLabel(manifestStore),
                        presenter.isAiGenerated(manifestStore),
                        presenter.getModifications(manifestStore),
                        presenter.getCapturedDate(manifestStore),
                        presenter.getSignedBy(manifestStore),
                        presenter.getSignedWith(manifestStore)
                );
                items.add(item);
            }
            MyAdapter adapter = new MyAdapter(items);
            list.setLayoutManager(new LinearLayoutManager(this));
            DividerItemDecoration divider = new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
            list.addItemDecoration(divider);
            list.setAdapter(adapter);
            message.setVisibility(View.GONE);
        } else if(status == C2paUtil.C2PAStatus.C2PA_INVALID_HASH) {
            list.setVisibility(View.GONE);
            message.setVisibility(View.VISIBLE);
            message.setText(R.string.c2pa_invalid_hash);
        } else if(status == C2paUtil.C2PAStatus.C2PA_INVALID_SIGNATURE) {
            list.setVisibility(View.GONE);
            message.setVisibility(View.VISIBLE);
            message.setText(R.string.c2pa_invalid_signature);
        } else { // shouldn't happen
            list.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
        }

        ImageView backButton = findViewById(R.id.back);
        backButton.setClickable(true);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public static String buildAddress(Address address) {
        if (address == null) return "";

        StringBuilder ret = new StringBuilder();

        if (address.getLocality() != null && !address.getLocality().isEmpty()) {
            ret.append(address.getLocality());
        }

        if (address.getAdminArea() != null && !address.getAdminArea().isEmpty()) {
            if (ret.length() > 0) ret.append(", ");
            ret.append(stateAbbreviation(address.getAdminArea()));
        }


        if (address.getCountryCode() != null && !address.getCountryCode().isEmpty()) {
            if (ret.length() > 0) ret.append(", ");

            if (address.getLocality() != null && !address.getLocality().isEmpty()) {
                ret.append(address.getCountryCode());
            } else {
                ret.append(address.getCountryName());
            }
        }

        return ret.toString();
    }

    public static String stateAbbreviation(String state) {
        switch (state) {
            case "Alabama":
                return "AL";
            case "Alaska":
                return "AK";
            case "Alberta":
                return "AB";
            case "American Samoa":
                return "AS";
            case "Arizona":
                return "AZ";
            case "Arkansas":
                return "AR";
            case "Armed Forces (AE)":
                return "AE";
            case "Armed Forces Americas":
                return "AA";
            case "Armed Forces Pacific":
                return "AP";
            case "British Columbia":
                return "BC";
            case "California":
                return "CA";
            case "Colorado":
                return "CO";
            case "Connecticut":
                return "CT";
            case "Delaware":
                return "DE";
            case "District Of Columbia":
                return "DC";
            case "Florida":
                return "FL";
            case "Georgia":
                return "GA";
            case "Guam":
                return "GU";
            case "Hawaii":
                return "HI";
            case "Idaho":
                return "ID";
            case "Illinois":
                return "IL";
            case "Indiana":
                return "IN";
            case "Iowa":
                return "IA";
            case "Kansas":
                return "KS";
            case "Kentucky":
                return "KY";
            case "Louisiana":
                return "LA";
            case "Maine":
                return "ME";
            case "Manitoba":
                return "MB";
            case "Maryland":
                return "MD";
            case "Massachusetts":
                return "MA";
            case "Michigan":
                return "MI";
            case "Minnesota":
                return "MN";
            case "Mississippi":
                return "MS";
            case "Missouri":
                return "MO";
            case "Montana":
                return "MT";
            case "Nebraska":
                return "NE";
            case "Nevada":
                return "NV";
            case "New Brunswick":
                return "NB";
            case "New Hampshire":
                return "NH";
            case "New Jersey":
                return "NJ";
            case "New Mexico":
                return "NM";
            case "New York":
                return "NY";
            case "Newfoundland":
                return "NF";
            case "North Carolina":
                return "NC";
            case "North Dakota":
                return "ND";
            case "Northwest Territories":
                return "NT";
            case "Nova Scotia":
                return "NS";
            case "Nunavut":
                return "NU";
            case "Ohio":
                return "OH";
            case "Oklahoma":
                return "OK";
            case "Ontario":
                return "ON";
            case "Oregon":
                return "OR";
            case "Pennsylvania":
                return "PA";
            case "Prince Edward Island":
                return "PE";
            case "Puerto Rico":
                return "PR";
            case "Quebec":
                return "PQ";
            case "Rhode Island":
                return "RI";
            case "Saskatchewan":
                return "SK";
            case "South Carolina":
                return "SC";
            case "South Dakota":
                return "SD";
            case "Tennessee":
                return "TN";
            case "Texas":
                return "TX";
            case "Utah":
                return "UT";
            case "Vermont":
                return "VT";
            case "Virgin Islands":
                return "VI";
            case "Virginia":
                return "VA";
            case "Washington":
                return "WA";
            case "West Virginia":
                return "WV";
            case "Wisconsin":
                return "WI";
            case "Wyoming":
                return "WY";
            case "Yukon Territory":
                return "YT";
            default:
                return state;
        }
    }

    public static String getAddress(Context context, ManifestStore manifestStore) {
        AtomicReference<String> retAddress = new AtomicReference<>(null);

        if (manifestStore != null && manifestStore.getAssertions() != null && manifestStore.getAssertions().getStdsExif() != null) {
            manifestStore.getAssertions().getStdsExif().forEach(it -> {
                try {
                    if (it.getExifData() != null && it.getExifData().getLongitude() != null && !it.getExifData().getLongitude().isEmpty()
                            && it.getExifData().getLatitude() != null && !it.getExifData().getLatitude().isEmpty()) {
                        double longitude = Double.parseDouble(it.getExifData().getLongitude());
                        double latitude = Double.parseDouble(it.getExifData().getLatitude());

                        if (!Geocoder.isPresent()) {
                            // geocoding not present, fallback to coordinates
                            retAddress.set(latitude + "," + longitude);
                            return;
                        }

                        Geocoder geocoder = new Geocoder(context);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            CountDownLatch countDownLatch = new CountDownLatch(1);
                            geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                                retAddress.set(buildAddress(addresses.get(0)));
                                countDownLatch.countDown();
                            });
                            countDownLatch.await(3, TimeUnit.SECONDS);
                        } else {
                            try {
                                retAddress.set(buildAddress(geocoder.getFromLocation(latitude, longitude, 1).get(0)));
                            } catch (Exception e) {
                                retAddress.set(latitude + "," + longitude);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.i(TAG, "exception:", e);
                }
            });
        }

        return retAddress.get();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.VH> {

        private final ArrayList<Item> mItems;

        public MyAdapter(ArrayList<Item> items) {
            mItems = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.c2pa_details, parent, false);
            return new VH(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Item item = mItems.get(position);
            holder.title.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            holder.progress.setVisibility(View.VISIBLE);
            if (item.thumbnail != null) {
                holder.thumbnail.setImageBitmap(item.thumbnail);
            } else {
                int thumbnail = R.drawable.no_thumbnail_image;
                if (item.type == C2PAPresenter.Type.Audio) {
                    thumbnail = R.drawable.no_thumbnail_audio;
                } else if (item.type == C2PAPresenter.Type.Video) {
                    thumbnail = R.drawable.no_thumbnail_video;
                }
                holder.thumbnail.setImageDrawable(
                        ContextCompat.getDrawable(
                                holder.thumbnail.getContext(),
                                thumbnail
                        )
                );
            }
            if (item.address != null) {
                holder.location_label.setVisibility(View.VISIBLE);
                holder.location_text.setVisibility(View.VISIBLE);
                holder.location_text.setText(item.address);
            } else {
                holder.location_label.setVisibility(View.GONE);
                holder.location_text.setVisibility(View.GONE);
            }
            holder.thumbnail_type.setText(item.typeLabel);
            holder.captured_with_label.setText(item.capturedWithLabel);
            holder.captured_with_text.setText(item.capturedWith);
            holder.captured_label.setText(item.capturedLabel);
            holder.aiwarning.setVisibility(item.isAiGenerated ? View.VISIBLE : View.GONE);
            if (item.modifications > 0) {
                holder.modifications_label.setVisibility(View.VISIBLE);
                holder.modifications_text.setVisibility(View.VISIBLE);
                holder.modifications_text.setText(String.valueOf(item.modifications));
                holder.signed_with_label.setVisibility(View.GONE);
                holder.signed_with_text.setVisibility(View.GONE);
            } else {
                holder.modifications_label.setVisibility(View.GONE);
                holder.modifications_text.setVisibility(View.GONE);
                holder.signed_with_label.setVisibility(View.VISIBLE);
                holder.signed_with_text.setVisibility(View.VISIBLE);
            }

            holder.captured_text.setText(item.capturedDateText);
            holder.signed_by_text.setText(item.signedByText);
            holder.signed_with_text.setText(item.signedWithText);
            holder.progress.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        private static class VH extends RecyclerView.ViewHolder {

            private TextView title;
            private ImageView thumbnail;
            private TextView thumbnail_type;
            private TextView aiwarning;
            private TextView captured_label;
            private TextView captured_text;
            private TextView location_label;
            private TextView location_text;
            private TextView captured_with_label;
            private TextView captured_with_text;
            private TextView modifications_label;
            private TextView modifications_text;
            private TextView signed_by_label;
            private TextView signed_by_text;
            private TextView signed_with_label;
            private TextView signed_with_text;
            private ProgressBar progress;


            public VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.title);
                thumbnail = itemView.findViewById(R.id.thumbnail);
                thumbnail_type = itemView.findViewById(R.id.thumbnail_type);
                aiwarning = itemView.findViewById(R.id.ai_warning);
                captured_label = itemView.findViewById(R.id.captured_label);
                captured_text = itemView.findViewById(R.id.captured_text);
                location_label = itemView.findViewById(R.id.location_label);
                location_text = itemView.findViewById(R.id.location_text);
                captured_with_label = itemView.findViewById(R.id.captured_with_label);
                captured_with_text = itemView.findViewById(R.id.captured_with_text);
                modifications_label = itemView.findViewById(R.id.modifications_label);
                modifications_text = itemView.findViewById(R.id.modifications_text);
                signed_by_label = itemView.findViewById(R.id.signed_by_label);
                signed_by_text = itemView.findViewById(R.id.signed_by_text);
                signed_with_label = itemView.findViewById(R.id.signed_with_label);
                signed_with_text = itemView.findViewById(R.id.signed_with_text);
                progress = itemView.findViewById(R.id.progress);
            }
        }
    }

    private static class Item {
        String address;
        Bitmap thumbnail;
        C2PAPresenter.Type type;
        String typeLabel;
        String capturedWith;
        String capturedWithLabel;
        String capturedLabel;
        boolean isAiGenerated;
        int modifications;
        String capturedDateText;
        String signedByText;
        String signedWithText;

        public Item(String address, Bitmap thumbnail, C2PAPresenter.Type type, String typeLabel, String capturedWith,
                    String capturedWithLabel, String capturedLabel, boolean isAiGenerated, int modifications,
                    String capturedDateText, String signedByText, String signedWithText) {
            this.address = address;
            this.thumbnail = thumbnail;
            this.type = type;
            this.typeLabel = typeLabel;
            this.capturedWith = capturedWith;
            this.capturedWithLabel = capturedWithLabel;
            this.capturedLabel = capturedLabel;
            this.isAiGenerated = isAiGenerated;
            this.modifications = modifications;
            this.capturedDateText = capturedDateText;
            this.signedByText = signedByText;
            this.signedWithText = signedWithText;
        }
    }
}
