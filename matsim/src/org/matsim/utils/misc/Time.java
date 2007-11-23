/* *********************************************************************** *
 * project: org.matsim.*
 * Time.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.misc;

import java.util.Calendar;
import java.util.Date;

/**
 * 
 * @author gunnar
 * 
 */
public class Time {

    // -------------------- CLASS VARIABLES --------------------

    public static final char DEFAULT_SEPARATOR = '-';

    public static final int SEC_PER_MIN = 60;

    public static final int MIN_PER_HOUR = 60;

    public static final int HOUR_PER_DAY = 24;

    public static final int SEC_PER_HOUR = SEC_PER_MIN * MIN_PER_HOUR;

    public static final int SEC_PER_DAY = SEC_PER_HOUR * HOUR_PER_DAY;

    private static Calendar calendar;

    private static Date startDate;

    // -------------------- STATIC CONSTRUCTOR --------------------

    static {
        calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        startDate = calendar.getTime();
    }

    // -------------------- SETTERS AND GETTERS --------------------

    /**
     * @param year
     *            e.g. 2006
     * @param month
     *            1(jan)..12(dec)
     * @param day
     *            1..28..31
     */
    public static void setStartDate(int year, int month, int day) {
        calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        startDate = calendar.getTime();
    }

    public static Date getStartDate() {
        return new Date(startDate.getTime());
    }

    // -------------------- SIMPLE MANIPULATIONS --------------------

    public static int projectOnto1stDay(int time_s) {
        int d = time_s / SEC_PER_DAY;
        return time_s - d * SEC_PER_DAY;
    }

    // -------------------- STRING PARSING --------------------

    public static int secFromStr(String timeStr, char separator) {
        String[] elements = timeStr.split("\\Q" + separator + "\\E");
        int h = Integer.parseInt(elements[0]);
        int m = Integer.parseInt(elements[1]);
        int s = (elements.length > 2) ? Integer.parseInt(elements[2]) : 0;
        return h * SEC_PER_HOUR + m * SEC_PER_MIN + s;
    }

    public static int secFromStr(String timeStr) {
        return secFromStr(timeStr, DEFAULT_SEPARATOR);
    }

    // -------------------- STRING FORMATTING --------------------

    public static String strFromSec(int time_s, char separator) {
        int h = time_s / 3600;
        time_s -= h * 3600;
        int m = time_s / 60;
        time_s -= m * 60;
        int s = time_s;
        return (h < 10 ? "0" : "") + h + separator + (m < 10 ? "0" : "") + m
                + separator + (s < 10 ? "0" : "") + s;
    }

    public static String strFromSec(int time_s) {
        return strFromSec(time_s, DEFAULT_SEPARATOR);
    }

    // -------------------- SECONDS TO Date --------------------

    public static Date dateFromSec(int time_s) {
        calendar.setTime(startDate);
        calendar.set(Calendar.SECOND, time_s);
        return calendar.getTime();
    }

    public static int secFromDate(Date date) {
        return (int) ((date.getTime() - startDate.getTime()) / 1000);
    }

    // ==================== TESTING ====================

    // creation

    private static void test1() {
        System.out.println("start date is " + Time.getStartDate());
    }

    // parsing

    private static void test2() {
        String str = "1-2"; // try different stuff here
        System.out.println(str + " has " + Time.secFromStr(str) + " seconds");
    }

    // formatting

    private static void test3() {
        int time_s = 60 * 60 * 24 * 10 + 1; // try different stuff here
        System.out.println(time_s + " is formatted as "
                + Time.strFromSec(time_s));
    }

    // Date conversion

    private static void test4() {
        int time_s = -60 * 60 * 24; // try different stuff here
        System.out.println(time_s + " as Date is " + Time.dateFromSec(time_s));
    }

    private static void test5() {
        // Date now = new Date(System.currentTimeMillis());
        Date now = new Date(Time.getStartDate().getTime() - 1000);
        System.out.println(now + " in seconds is " + Time.secFromDate(now));
    }

    private static void test6() {
        Time.setStartDate(1976, 12, 7);
        System.out.println("start date ist " + Time.getStartDate());
    }

    // main

    public static void main(String[] args) {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
    }

}
