/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.analysis;

import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.analysis.modalsplit.ModalSplitAnalyser;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TripsPerModeAnalysisB {
public static void main(String[] args) {
	String runId = "25pct.r06";
	String plansFile = "D:/runs-svn/bvg_intermodal/software-lieferung/bvg.run191.25pct.100.plans.filtered.selected.noRoutes.xml.gz";
	String eventsF = "D:/runs-svn/bvg_intermodal/"+runId+"/"+runId+".output_events.xml.gz";
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile(plansFile);
	ModalSplitAnalyser ma_a = new ModalSplitAnalyser(scenario);

	EventsManager events = EventsUtils.createEventsManager();
	events.addHandler(ma_a);

	new MatsimEventsReader(events).readFile(eventsF);
	System.out.println("Mode stats all");
	for (Entry<String, MutableInt> e : ma_a.getTripsPerMode().entrySet() ){
		System.out.println(e.getKey()+"\t "+e.getValue().toString());
	}

	
	
}
}
