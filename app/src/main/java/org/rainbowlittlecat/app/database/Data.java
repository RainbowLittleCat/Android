/*
 * Changed from
 *     https://github.com/googlecodelabs/android-room-with-a-view/blob/master/app/src/main/java/com/example/android/roomwordssample/Word.java
 *
 * Tutorial
 *     https://codelabs.developers.google.com/codelabs/android-room-with-a-view/#3
 *
 * Copyright (C) 2017 Google Inc.
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

package org.rainbowlittlecat.app.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = DatabaseConstants.TABLE_NAME)
public class Data {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseConstants.COLUMN_DATE_TIME)
    private String mDateTime;

    @ColumnInfo(name = DatabaseConstants.COLUMN_AIR_QUALITY)
    private float mAirQuality;

    @ColumnInfo(name = DatabaseConstants.COLUMN_TEMPERATURE)
    private float mTemperature;

    @ColumnInfo(name = DatabaseConstants.COLUMN_HUMIDITY)
    private float mHumidity;

    public Data(@NonNull String dateTime, float airQuality, float temperature, float humidity) {
        mDateTime = dateTime;
        mAirQuality = airQuality;
        mTemperature = temperature;
        mHumidity = humidity;
    }

    public String getDateTime() {
        return mDateTime;
    }

    public float getAirQuality() {
        return mAirQuality;
    }

    public float getTemperature() {
        return mTemperature;
    }

    public float getHumidity() {
        return mHumidity;
    }
}
