/* *********************************************************************** *
 * project: org.matsim.*
 * DateConversion.java
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

package teach.multiagent07.util;

import java.util.StringTokenizer;

public class DateConversion {
	
	   public static final int secondsFromString(String timeStr) {
	       int sec = 0;
	       if (timeStr != null) {
		       StringTokenizer  stringToki = new StringTokenizer(timeStr, ":", false);
		       sec += 3600*Integer.parseInt(stringToki.nextToken());
		       sec += 60*Integer.parseInt(stringToki.nextToken());
		       // might be 05:30 kind of token omitting second
		       if (stringToki.hasMoreTokens())
		         sec += Integer.parseInt(stringToki.nextToken());
	       }
	       return sec;
	   }
	
	   public static final String stringFromSeconds(int s, String separator) {
	       int h = s / 3600;
	       s -= h * 3600;
	       int m = s / 60;
	       s -= m * 60;
	       String res = String.format("%02d%s%02d%s%02d", h, separator, m, separator, s);
	       return res;
	   } 
}
