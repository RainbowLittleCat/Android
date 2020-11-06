package org.rainbowlittlecat.app.database;

import android.provider.BaseColumns;

/**
 * BluetoothConstants for database.
 */
public interface DatabaseConstants extends BaseColumns {
    /**
     * Table names
     */
    String TABLE_NAME = "data_table";

    /**
     * Columns
     */
    String COLUMN_DATE_TIME = "date_time";
    String COLUMN_AIR_QUALITY = "air_quality";
    String COLUMN_TEMPERATURE = "temperature";
    String COLUMN_HUMIDITY = "humidity";
}
