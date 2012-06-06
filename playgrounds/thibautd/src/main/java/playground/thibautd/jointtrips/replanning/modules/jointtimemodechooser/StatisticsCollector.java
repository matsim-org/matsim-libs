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
package playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser;

import java.io.BufferedWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.matsim.core.utils.io.IOUtils;

import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;

/**
 * Collects statistics to help debugging
 * @author thibautd
 */
class StatisticsCollector {
	private static final Logger log =
		Logger.getLogger(StatisticsCollector.class);

	private final Map<String, Set<String>> routingModuleFactoryClasses = new HashMap<String, Set<String>>();

	public synchronized  void notifyTripRouterFactory(final TripRouterFactory factory) {
		for (Map.Entry<String, RoutingModuleFactory> e : factory.getRoutingModuleFactories().entrySet()) {
			Set<String> classes = routingModuleFactoryClasses.get( e.getKey() );
			if (classes == null) {
				classes = new TreeSet<String>();
				routingModuleFactoryClasses.put( e.getKey() , classes );
			}
			classes.add( e.getValue().getClass().toString() );
		}
	}

	public void dumpStatistics(final String fileName) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter( fileName );

			writer.write( "class of the routing modules used for travel time estimation:" );
			for (Map.Entry<String, Set<String>> entry : routingModuleFactoryClasses.entrySet()) {
				writer.newLine();
				writer.write( "\t->mode "+entry.getKey()+":" );
				for (String c : entry.getValue()) {
					writer.newLine();
					writer.write( "\t\t->"+c );
				}
			}

			writer.close();
		}
		catch (Exception e) {
			log.warn( "problem while dumping statistics" , e );
		}
	}
}

