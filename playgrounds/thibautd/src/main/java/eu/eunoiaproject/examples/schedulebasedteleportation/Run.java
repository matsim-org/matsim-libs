/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.examples.schedulebasedteleportation;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author thibautd
 */
public class Run {
	private static final Logger log =
		Logger.getLogger(Run.class);


	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		// This allows to get all log messages in the log file.
		// Otherwise, one only gets the messages happening after Controler
		// initialization
		OutputDirectoryLogging.catchLogEntries();

		final Config config = loadConfig( configFile );
		final Scenario scenario = loadScenario( config );

		final Controler controler = new Controler( scenario );

		// this is where the magic happens
		controler.setTripRouterFactory(
				new ScheduleBasedTripRouterFactory(
					scenario ) );

		controler.run();
	}

	private static Scenario loadScenario( final Config config ) {
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		// if actual simulation of transit is disabled, the transit schedule
		// is not loaded automatically: we need to do it by hand
		if ( !config.transit().isUseTransit() ) {
			((ScenarioImpl) scenario).createTransitSchedule();
			log.info( "read schedule from "+config.transit().getTransitScheduleFile() );
			new TransitScheduleReader( scenario ).readFile( config.transit().getTransitScheduleFile() );

			// this is not necessary in the vast majority of applications.
			if ( config.transit().getTransitLinesAttributesFile() != null ) {
				log.info("loading transit lines attributes from " + config.transit().getTransitLinesAttributesFile());
				new ObjectAttributesXmlReader( scenario.getTransitSchedule().getTransitLinesAttributes() ).parse(
						config.transit().getTransitLinesAttributesFile() );
			}
			if ( config.transit().getTransitStopsAttributesFile() != null ) {
				log.info("loading transit stop facilities attributes from " + config.transit().getTransitStopsAttributesFile() );
				new ObjectAttributesXmlReader( scenario.getTransitSchedule().getTransitStopsAttributes() ).parse(
						config.transit().getTransitStopsAttributesFile() );
			}
		}
		else {
			log.warn( "Transit will be simulated! This is not the purpose of this script" );
		}

		return scenario;
	}

	private static Config loadConfig( final String configFile ) {
		final Config config = ConfigUtils.loadConfig( configFile );

		// this is normally done in the Controler if transit is enabled
		final ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);

		transitActivityParams.setOpeningTime(0.) ;
		transitActivityParams.setClosingTime(0.) ;

		config.planCalcScore().addActivityParams(transitActivityParams);

		return config;
	}
}
