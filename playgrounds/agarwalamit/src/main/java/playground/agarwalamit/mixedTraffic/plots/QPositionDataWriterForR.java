/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.agarwalamit.mixedTraffic.plots;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class QPositionDataWriterForR {

	private static String outputDir ="../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/"
//			+ "run313/car_2lanes_60/";
			+ "run308/carBike/carBikePassing/";
	private static String eventFile = outputDir+"/events.xml";
	private static String networkFile=outputDir+"/network.xml";
	
	private static Scenario scenario;
	private static QueuePositionCalculationHandler calculationHandler;

	private final static Logger LOG = Logger.getLogger(QPositionDataWriterForR.class);

	public void run(){
		scenario  = LoadMyScenarios.loadScenarioFromNetwork(networkFile);

		calculationHandler = new QueuePositionCalculationHandler(scenario,outputDir);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(calculationHandler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventFile);
		calculationHandler.closeWriter();
		LOG.info("Writing file(s) is finished.");
	}
	
	public static void main(String[] args) {
		new QPositionDataWriterForR().run();
	}
}