/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.analysis.kai;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;

/**
 * @author nagel
 *
 */
public class RunKNEventsAnalyzer {

	public static void main(String[] args) {

		if ( args.length < 3 ) {
			System.out.println("Usage: cmd eventsFile popFile netFile [popAttrFile] [tollFile] [futureTollFile]. Aborting ..." ) ;
			System.exit(-1);
		}

		String eventsFilename = args[0] ;
		String populationFilename = args[1] ;
		String networkFilename = args[2] ;

		String popAttrFilename = null ;
		if ( args.length > 3 && args[3]!=null ) {
			popAttrFilename = args[3] ;
		}

		String tollFilename = null ;
		if ( args.length > 4 && args[4]!=null ) {
			tollFilename = args[4] ;
		}

		String otherLinksFilename = null ;
		if ( args.length > 5 && args[5]!=null ) {
			otherLinksFilename = args[5] ;
		}

		// ===

		Config config = ConfigUtils.createConfig() ;

		String[] modes ={"car","commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.routing().setNetworkModes(Arrays.asList(modes));

		config.network().setInputFile( networkFilename );
		config.plans().setInputFile( populationFilename );
		config.plans().setInputPersonAttributeFile( popAttrFilename );
        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(tollFilename);

		// ===

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

//		((ScenarioImpl)scenario).createVehicleContainer() ;
//		GautengControler_subpopulations.createVehiclePerPerson(scenario);

		// ===

		EventsManager events = new EventsManagerImpl() ;

		Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler();
		events.addHandler(vehicle2Driver);

		final KNAnalysisEventsHandler.Builder builder = new KNAnalysisEventsHandler.Builder(scenario) ;
		builder.setOtherTollLinkFile( otherLinksFilename );
		final KNAnalysisEventsHandler calcLegTimes = builder.build();

		events.addHandler( calcLegTimes );

		new MatsimEventsReader(events).readFile(eventsFilename) ;

//		String myDate = date.getYear() + "-" + date.getMonthOfYear() + "-" + date.getDayOfMonth() + "-" +
//				date.getHourOfDay() + "h" + minute ;
		String myDate = "" ;

		calcLegTimes.writeStats(myDate + "_stats_");
	}

}
