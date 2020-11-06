package org.rainbowlittlecat.app.voice;

import android.content.Context;

import org.rainbowlittlecat.app.R;

/**
 * This class helps identify specific sentences in a message.
 */
public class LanguageTeacher {
    private static final int TARGET_DISABLE = 0;
    private static final int TARGET_ENABLE = 1;
    private static final int TARGET_ACTION = 2;
    private static final int TARGET_AIR_QUALITY = 3;
    private static final int TARGET_TEMPERATURE = 4;
    private static final int TARGET_WET = 5;

    private static final int TARGET_USER_SAY_HELLO = 6;

    private static final int TARGET_I_UNDERSTAND_REPLY = 7;
    private static final int TARGET_I_DO_NOT_KNOW_REPLY = 8;
    private static final int TARGET_HELLO_REPLY = 9;

    private static final int TARGET_MEWS = 10;
    private static final int TARGET_FUCK = 11;

    private static final String[][] dataBase = {
            /*
             * keywords user might say.
             */
            {"停止", "關閉", "關掉", "停", "關"},                     // 0: turn off
            {"打開一下", "開啟", "啟動", "打開", "開"},                // 1: turn on
            {"沙菌", "殺菌", "滅菌", "除菌", "除臭", "滅臭", "消臭"},   // 2: action
            {"空氣品質", "空氣", "pm2.5", "PM2.5", "pm", "PM",
                    "Pm", "pM"},                                    // 3: air quality
            {"溫度", "氣溫"},                                        // 4: temperature
            {"10度", "濕", "師", "十", "時", "私", "石度", "蝨",
                    "失", "獅", "之", "吃", "師素", "之度", "濕度",
                    "師度", "吃吐", "私度", "速度"},                  // 5: wet

            {"哈囉", "hello", "Hello", "Hi", "hi", "hey", "Hey",
                    "哈囉小貓", "哈囉彩虹小貓", "Hello小貓",
                    "hello小貓", "Hello彩虹小貓", "hello彩虹小貓",
                    "hello", "Hello", "哈嘍小貓", "哈嘍彩虹小貓"},    // 6: user say hello

            /*
             * below is for reply
             */
            {"好喔", "好 馬上辦", "交給我吧", "是的 船長", "OK"},      // 7: ok reply
            {"我聽不懂", "可以再說一次嗎", "再說一次好嗎", "太小聲嘍"},  // 8: i don't know what you said reply
            {"哈囉", "哈嘍", "Hi", "哈哈", "你好", "哈哈 怎麼了"},     // 9: hello reply

            {"學貓叫", "我們一起", "一起喵", "貓叫"},                  // 10: play mews in main activity
            {"媽", "幹拎娘勒", "幹", "幹大事", "大事",
                    "操", "糙", "他媽", "乾", "大"}                  // 11: play fuck_big_thing in main activity
    };

    private static final String CAT_MEWS = "糾咪";
    private static final int MAX_MEWS = 3;

    /**
     * Before sending message to remote device, check if the message match the information depending on the data base.
     */
    public String searchKeyWords(String str) {
        System.out.println("home work : " + str);

        String enableResult;
        String disableResult;

        int action = 0;
        /* function state.
         *   1 -> enable.
         *   0 -> remain the same.
         *  -1 -> disable.
         */

        disableResult = str;
        while (containKeywords(TARGET_DISABLE, disableResult)) {
            System.out.println("has -> 關掉");
            // pre order
            action = -1;

            String nextMessage = getStringAfterKeyWords(TARGET_DISABLE, disableResult);

            System.out.println("next -> " + nextMessage);

            if (startsWithKeyWords(nextMessage)) {
                System.out.println("and then 關掉殺菌");
                disableResult = getStringAfterKeyWords(TARGET_ACTION, nextMessage);
                action = -1;
            } else {
                break;
            }
        }

        enableResult = str;
        while (containKeywords(TARGET_ENABLE, enableResult)) {
            System.out.println("has -> 開啟");
            // pre order
            action = 1;

            String nextMessage = getStringAfterKeyWords(TARGET_ENABLE, enableResult);

            System.out.println("next -> " + nextMessage);

            if (startsWithKeyWords(nextMessage)) {  //開啟殺菌
                System.out.println("and then 殺菌");
                enableResult = getStringAfterKeyWords(TARGET_ACTION, nextMessage);
                action = 1;
            } else {
                break;
            }
        }

        // if the action keyword is in the head of that string
        if (action == 0 && (startsWithKeyWords(str) || containKeywords(TARGET_ACTION, str))) {
            System.out.println("ensure 殺菌");
            action = 1;
        }

        if (action == 1) {
            System.out.println("result : 殺菌");
            return "o";
        } else if (action == -1) {
            System.out.println("result : 關閉");
            return "f";
        }

        // want cat say something
        if (containKeywords(TARGET_AIR_QUALITY, str)) {
            return "a";
        } else if (containKeywords(TARGET_TEMPERATURE, str)) {
            return "t";
        } else if (containKeywords(TARGET_WET, str)) {
            return "w";
        }

        // if user only say hello
        if (containKeywords(TARGET_USER_SAY_HELLO, str)) {
            return "h";
        }

        // if user wanna learn to mews
        if (containKeywords(TARGET_MEWS, str)) {
            return "m";
        }

        // if user wanna to fuck big thing
        if (containKeywords(TARGET_FUCK, str)) {
            return "b";
        }

        // action == 0
        System.out.println("result : 聽不懂");
        return "d";
    }

    /**
     * Check if that given string contains any of the keywords inside the database
     *
     * @return if it contains or not
     */
    private boolean containKeywords(int type, String str) {
        for (int i = 0; i < dataBase[type].length; i++) {
            String data = dataBase[type][i];
            int position = str.indexOf(data);

            if (position > -1) {
                return true;
            }
        }

        return false;
    }

    /**
     * To check if that given string contain one of the items in the target database.
     *
     * @return empty if it do not contain. If it contains, return string after that keywords.
     */
    private String getStringAfterKeyWords(int type, String str) {
        String info = "";

        for (int i = 0; i < dataBase[type].length; i++) {
            String data = dataBase[type][i];
            int position = str.indexOf(data);

            if (position > -1) {
                //Contain the data. Return the string after "data".
                String trailPart = str.substring(position + data.length());
                System.out.println("trailPart = " + trailPart);
                info = trailPart;
                break;
            }
        }

        return info;
    }

    /**
     * Check if the given string starts with key words inside the database.
     *
     * @return if that string starts with key words or not.
     */
    private boolean startsWithKeyWords(String str) {
        for (int i = 0; i < dataBase[LanguageTeacher.TARGET_ACTION].length; i++) {
            if (str.startsWith(dataBase[LanguageTeacher.TARGET_ACTION][i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * if not connected to device, say this
     */
    public static String deviceNotFoundReply(Context context) {
        String[] arr = context.getResources().getStringArray(R.array.not_connect_to_device_reply);
        return arr[(int) (Math.random() * arr.length)];
    }

    /**
     * "i understand" reply
     */
    public static String IUnderstandReply() {
        return dataBase[TARGET_I_UNDERSTAND_REPLY][(int) (Math.random() * dataBase[TARGET_I_UNDERSTAND_REPLY].length)];
    }

    /**
     * "i don't know reply
     */
    public static String IDoNotKnowReply() {
        return dataBase[TARGET_I_DO_NOT_KNOW_REPLY][(int) (Math.random() * dataBase[TARGET_I_DO_NOT_KNOW_REPLY].length)];
    }

    /**
     * reply user if he/she only say hello
     */
    public static String sayHello() {
        return dataBase[TARGET_HELLO_REPLY][(int) (Math.random() * dataBase[TARGET_HELLO_REPLY].length)];
    }

    /**
     * get "mews"
     */
    public static String getSeveralCatMews() {
        StringBuilder str = new StringBuilder(CAT_MEWS);
        int howMuch = (int) (Math.random() * MAX_MEWS);

        for (int i = 0; i < howMuch; i++) {
            str.append(CAT_MEWS);
        }

        return str.toString();
    }
}
