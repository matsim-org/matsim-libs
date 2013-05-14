/* *********************************************************************** *
 * project: org.matsim.*
 * StatisticsCollector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.utils.io.IOUtils;


/**
 * Collects statistics to help debugging
 * @author thibautd
 */
class StatisticsCollector {
	private static final Logger log =
		Logger.getLogger(StatisticsCollector.class);

	private final Map<String, String> routingModuleClasses = new HashMap<String, String>();

	public synchronized  void notifyTripRouterFactory(final TripRouterFactory factory) {
		TripRouter router = factory.instantiateAndConfigureTripRouter();
		for (String mode : router.getRegisteredModes()) {
			routingModuleClasses.put(
					mode,
					""+router.getRoutingModule( mode ).getClass() );
		}
	}

	public void dumpStatistics(final String fileName) {
		try {
			BufferedWriter writer = IOUtils.getAppendingBufferedWriter( fileName );

			writer.newLine();
			writer.write( "###################################################################" );
			writer.newLine();
			writer.write( "stack trace:");
			writer.newLine();
			for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
				writer.write( "\t"+s.toString() );
				writer.newLine();
			}
			writer.newLine();

			writer.write( "class of the routing modules used for travel time estimation:" );
			for (Map.Entry<String, String> entry : routingModuleClasses.entrySet()) {
				writer.newLine();
				writer.write( "\t->mode "+entry.getKey()+": "+entry.getValue() );
			}
			writer.newLine();

			writer.close();
		}
		catch (Exception e) {
			log.warn( "problem while dumping statistics" , e );
		}
	}
}

