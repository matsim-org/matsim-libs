/* *********************************************************************** *
 * project: org.matsim.*
 * LoggerUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.util;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Utility functions for logging.
 *
 * @author illenberger
 *
 */
public class LoggerUtils {

	private static boolean disallowVerbose = true;

	private static PatternLayout defaultLayout;

	/**
	 * Debugging purpose: Allows to disable the effect of {@link #setVerbose(boolean)}. Use this function to ensure
	 * that all messages are logged.
	 *
	 * @param disable if <tt>true</tt> {@code #setVerbose(false)} will have no effect
	 */
	public static void setDisableVerbose(boolean disable) {
		disallowVerbose = disable;
	}

	/**
	 * Allows to quickly switch between logging at all levels and logging only from warn-level. Use this function to
	 * temporary suppress extensive logging.
	 *
	 * @param verbose if <tt>true</tt> logger level is set to {@link Level#ALL}, otherwise level is set to {@link
	 * Level#WARN}.
	 */
	public static void setVerbose(boolean verbose) {
		if(verbose)
			Logger.getRootLogger().setLevel(Level.ALL);
		else if(!verbose && !disallowVerbose){
			Logger.getRootLogger().setLevel(Level.WARN);
		}
			
	}

	/**
	 * Remove the line break form the logger pattern. Subsequent writes to stdout are printed to the same line.
	 */
	public static void disableNewLine() {
		Appender appender = Logger.getRootLogger().getAppender("stdout");
		defaultLayout = (PatternLayout)appender.getLayout();
		PatternLayout newLayout = new PatternLayout(defaultLayout.getConversionPattern().replace("%n",""));
		appender.setLayout(newLayout);
	}

	/**
	 * Appends a line break to the logger pattern (default behavior).
	 */
	public static void enableNewLine() {
		Appender appender = Logger.getRootLogger().getAppender("stdout");
		appender.setLayout(defaultLayout);
	}
}
