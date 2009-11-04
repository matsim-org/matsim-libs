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

package playground.balmermi.toggenburg;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoader;

import playground.balmermi.datapuls.modules.FacilitiesWriteTables;
import playground.balmermi.datapuls.modules.PopulationWriteTable;
import playground.balmermi.toggenburg.modules.FacilitiesCount;


public class Analysis {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Analysis.class);

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		
		log.info("loading scenario...");
		ScenarioImpl scenario = new ScenarioLoader(args[0]).loadScenario();
		log.info("done.");
		
		log.info("extracting output directory... ");
		String outdir = scenario.getConfig().facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("=> "+outdir);
		log.info("done.");
		
		FacilitiesCount fc = new FacilitiesCount(); 
		
		// Analyze facilities
		fc.run(scenario.getActivityFacilities());
		
		// Analyze sinlge facility (it's a stupid example)
		fc.run(scenario.getActivityFacilities().getFacilities().values().iterator().next());
	}
}
