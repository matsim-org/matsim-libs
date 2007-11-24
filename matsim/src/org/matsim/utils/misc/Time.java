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

/**
 * @author gunnar
 */
public class Time {

    // -------------------- CLASS VARIABLES --------------------

    public static final char DEFAULT_SEPARATOR = '-';

    // -------------------- STRING PARSING --------------------

    public static int secFromStr(String timeStr, char separator) {
        String[] elements = timeStr.split("\\Q" + separator + "\\E");
        int h = Integer.parseInt(elements[0]);
        int m = Integer.parseInt(elements[1]);
        int s = (elements.length > 2) ? Integer.parseInt(elements[2]) : 0;
        return h * 3600 + m * 60 + s;
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

}
