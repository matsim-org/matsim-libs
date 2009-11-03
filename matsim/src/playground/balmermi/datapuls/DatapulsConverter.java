/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventHandler.java
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

package playground.balmermi.datapuls;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoader;

import playground.balmermi.datapuls.modules.FacilitiesWriteTables;
import playground.balmermi.datapuls.modules.LinkTablesEventHandler;
import playground.balmermi.datapuls.modules.PopulationWriteTable;


public class DatapulsConverter {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(DatapulsConverter.class);

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		
		log.info("gathering time bin size...");
		int timeBinSize = Integer.parseInt(args[1]);
		log.info("=> timeBinSize: "+timeBinSize);
		log.info("done.");
		
		log.info("loading scenario...");
		ScenarioImpl scenario = new ScenarioLoader(args[0]).loadScenario();
		log.info("done.");

		log.info("extracting output directory... ");
		String outdir = scenario.getConfig().facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("=> "+outdir);
		log.info("done.");
		
		new FacilitiesWriteTables().run(scenario.getActivityFacilities(),outdir);
		new PopulationWriteTable(scenario.getActivityFacilities()).run(scenario.getPopulation(),outdir);

		EventsImpl events = new EventsImpl();
		
		LinkTablesEventHandler handler = new LinkTablesEventHandler(timeBinSize,"../../output",scenario.getPopulation());
		
		events.addHandler(handler);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
		eventsReader.readFile("../../input/150.events.txt.gz");
	}
}
