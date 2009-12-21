/* *********************************************************************** *
 * project: org.matsim.*
 * DebugTools.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils;

/**
 * @author yu
 * 
 */
public class DebugTools {
	/**
	 * @param e
	 * @return the line number of the source line containing the execution point
	 *         represented by this stack trace element, or a negative number if
	 *         this information is unavailable.
	 */
	public static int getLineNumber(Exception e) {
		StackTraceElement[] trace = e.getStackTrace();
		if (trace == null || trace.length == 0)
			return -1; //      
		return trace[0].getLineNumber();
	}
}
