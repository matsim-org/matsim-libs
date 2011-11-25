/* *********************************************************************** *
 * project: org.matsim.*
 * MoreIOUtils.java
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
package playground.thibautd.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

/**
 * Defines some useful i/o related methods, which are not part of the
 * core MATSim IOUtils class.
 * @author thibautd
 */
public class MoreIOUtils {
	private MoreIOUtils() {
		//no instanciation 
	}

	/**
	 * creates an output directory if it does not exists, and creates a logfile.
	 */
	public static void initOut( String outputDir ) {
		try {
			// create directory if does not exist
			if (!outputDir.endsWith("/")) {
				outputDir += "/";
			}
			File outputDirFile = new File(outputDir);
			if (!outputDirFile.exists()) {
				outputDirFile.mkdirs();
			}

			// init logFile
			CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
			Logger.getRootLogger().addAppender(appender);

			IOUtils.initOutputDirLogging(
				outputDir,
				appender.getLogEvents());
		} catch (IOException e) {
			// do NOT continue without proper logging!
			throw new RuntimeException("error while creating log file",e);
		}
	}


}

