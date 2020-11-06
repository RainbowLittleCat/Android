package org.rainbowlittlecat.app.bluetooth;

import java.util.Calendar;

/**
 * Help MainActivity update data from BluetoothService
 */
public class DataUpdateHelper {
    /**
     * Update listener
     */
    private UpdateListener mUpdateListener;

    /**
     * Update thread
     */
    private UpdateThread mUpdateThread;

    /**
     * Check if this class is just start up
     */
    private boolean isJustWakeUp = true;

    /**
     * Record last time request all day data
     */
    private long lastTimeRequestAllDayData = 0;

    /**
     * Delay time
     */
    private static final int DELAY_TRASH_TIME = 500;
    private static final int REFRESH_TIME = 1000;
    public static final int HALF_AN_HOUR = 1800000;

    /**
     * Constructor
     */
    DataUpdateHelper() {
        mUpdateThread = new UpdateThread();
    }

    /**
     * Start update thread
     */
    void start() {
        if (mUpdateThread == null) {
            mUpdateThread = new UpdateThread();
        }
        mUpdateThread.start();
    }

    /**
     * Stop update thread
     */
    void stop() {
        if (mUpdateThread != null) {
            mUpdateThread.forceStop();
            mUpdateThread = null;
        }
    }

    /**
     * if this class is waiting for unexpected data
     */
    boolean isWaitingForUnexpectedData() {
        return isJustWakeUp;
    }

    /**
     * Set UpdateListener
     */
    void setUpdateListener(UpdateListener listener) {
        mUpdateListener = listener;
    }

    /**
     * Thread to count the time, and send notification to main activity through UpdateListener
     */
    private class UpdateThread extends Thread {
        private boolean isContinued = true;

        @Override
        public void run() {
            try {
                while (isContinued) {
                    if (isJustWakeUp) {
                        // waiting for unexpected data
                        Thread.sleep(DELAY_TRASH_TIME);
                        if (isContinued) {
                            isJustWakeUp = false;
                            mUpdateListener.onRequestAllDayData();
                            // record now time
                            lastTimeRequestAllDayData = Calendar.getInstance().getTimeInMillis();
                        }
                    } else {
                        Thread.sleep(REFRESH_TIME);
                        if (isContinued) {
                            long nowTime = Calendar.getInstance().getTimeInMillis();
                            if ((nowTime - lastTimeRequestAllDayData) >= HALF_AN_HOUR) {
                                mUpdateListener.onRequestAllDayData();
                                lastTimeRequestAllDayData = Calendar.getInstance().getTimeInMillis();
                            } else {
                                mUpdateListener.onRequestRealtimeData();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void forceStop() {
            isContinued = false;
        }
    }

    /**
     * Interface to help updating data.
     */
    public interface UpdateListener {
        void onRequestAllDayData();

        void onRequestRealtimeData();
    }
}
