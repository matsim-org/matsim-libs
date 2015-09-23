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
package playground.johannes.coopsim;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * @author illenberger
 *
 */
public class LoggerUtils {

	private static boolean disallowVerbose = true;

	private static PatternLayout defaultLayout;
	
	public static void setDisallowVerbose(boolean disallow) {
		disallowVerbose = disallow;
	}
	
	public static void setVerbose(boolean verbose) {
		if(verbose)
			Logger.getRootLogger().setLevel(Level.ALL);
		else if(!verbose && !disallowVerbose){
			Logger.getRootLogger().setLevel(Level.WARN);
		}
			
	}

	public static void disableNewLine() {
		Appender appender = Logger.getRootLogger().getAppender("stdout");
		defaultLayout = (PatternLayout)appender.getLayout();
		PatternLayout newLayout = new PatternLayout(defaultLayout.getConversionPattern().replace("%n",""));
		appender.setLayout(newLayout);
	}

	public static void enableNewLine() {
		Appender appender = Logger.getRootLogger().getAppender("stdout");
		appender.setLayout(defaultLayout);
	}
}
