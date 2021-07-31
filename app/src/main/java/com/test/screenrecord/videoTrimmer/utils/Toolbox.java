package com.test.screenrecord.videoTrimmer.utils;

import java.text.DecimalFormat;

public class Toolbox {
    public static String converTime(String timeIn) {
        DecimalFormat precision = new DecimalFormat("00");
        int time = 0;
        try {
            time = (int) Float.parseFloat(timeIn);
        } catch (Exception e) {
            return "00:00";
        }
        if (time == 0) {
            return "00:00";
        }
        if (time < 60) {
            return "00" + ":" + precision.format(time);
        } else {
            int minuteN = time / 60;
            int secoundN = time % 60;
            if (minuteN < 60) {
                return precision.format(minuteN) + ":" + precision.format(secoundN);
            } else {
                int hourN = minuteN / 60;
                return precision.format(hourN) + ":" + precision.format((minuteN % 60)) + ":" + precision.format(secoundN);
            }
        }
    }
}
