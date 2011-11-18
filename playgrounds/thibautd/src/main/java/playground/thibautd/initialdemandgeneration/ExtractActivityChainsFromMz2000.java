/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractActivityChainsFromMz2000.java
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
package playground.thibautd.initialdemandgeneration;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

import playground.thibautd.initialdemandgeneration.activitychainsextractor.MzActivityChainsExtractor;

/**
 * executable class to extract activity chains from MZ2000
 * @author thibautd
 */
public class ExtractActivityChainsFromMz2000 {
	/**
	 * usage: ExtractActivityChainsFromMz2000 zpFile wgFile etFile outDir startDay endDay
	 */
	public static void main(final String[] args) {
		initOut( args[ 3 ] );
		MzActivityChainsExtractor extractor = new MzActivityChainsExtractor();
		Scenario scen = extractor.run(
				args[ 0 ],
				args[ 1 ],
				args[ 2 ],
				args[ 4 ],
				args[ 5 ]);
		(new PopulationWriter(scen.getPopulation(),
							  scen.getNetwork())).write(
						  args[ 3 ] + "/actchains-dow-"+args[ 4 ]+"-"+args[ 5 ]+".xml.gz" );
	}

	private static void initOut( String outputDir ) {
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

