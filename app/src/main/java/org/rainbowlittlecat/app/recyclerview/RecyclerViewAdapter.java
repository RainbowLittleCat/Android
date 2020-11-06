/*
 * Changed from
 *     https://github.com/googlearchive/android-RecyclerView/blob/master/Application/src/main/java/com/example/android/recyclerview/CustomAdapter.java
 *
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rainbowlittlecat.app.recyclerview;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.rainbowlittlecat.app.R;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder> {
    private ArrayList<String> mDataSet;

    private OnItemClickedListener mListener;

    public RecyclerViewAdapter(ArrayList<String> dataSet) {
        mDataSet = dataSet;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.device_name, parent, false);
        return new CustomViewHolder(v);
    }

    // Replace the content of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        holder.textView.setText(mDataSet.get(position));
        holder.itemView.setOnClickListener(v -> mListener.onItemClicked(position));
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mListener = listener;
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        CustomViewHolder(TextView itemView) {
            super(itemView);
            textView = itemView;
        }
    }

    public interface OnItemClickedListener {
        void onItemClicked(int position);
    }
}
