package org.rainbowlittlecat.app.chart;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Format x axis value to time and date
 * able to format chart's x axis in seconds or half an hour
 */
public class XAxisValueFormatter extends ValueFormatter {
    /**
     * SimpleDateFormat
     */
    private SimpleDateFormat simpleDateFormat;

    /**
     * Constructor
     */
    XAxisValueFormatter() {
        simpleDateFormat = new SimpleDateFormat("dd HH:mm", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        return simpleDateFormat.format(new Date((long) value));
    }

}
