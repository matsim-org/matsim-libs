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
package playground.jbischoff.wobscenario.analysis;

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
public class TripsPerModeAnalysis {
public static void main(String[] args) {
	
	String plansF = "D:/runs-svn/vw_rufbus/run122.100/run122.100.output_plans.xml.gz";
	String eventsF = "D:/runs-svn/vw_rufbus/run122.100/run122.100.output_events.xml.gz";
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile(plansF);
	ModalSplitAnalyser ma_wb_wb = new ModalSplitAnalyser(scenario);
	ModalSplitAnalyser ma_a = new ModalSplitAnalyser(scenario);
	ModalSplitAnalyser ma_wb_ = new ModalSplitAnalyser(scenario);
	ModalSplitAnalyser bs_bs = new ModalSplitAnalyser(scenario);
	ModalSplitAnalyser bs_wb = new ModalSplitAnalyser(scenario);
	EventsManager events = EventsUtils.createEventsManager();
	events.addHandler(ma_a);
	events.addHandler(ma_wb_wb);
	events.addHandler(ma_wb_);
	events.addHandler(bs_wb);
	events.addHandler(bs_bs);
	ma_wb_wb.addAgentGroup("WB_WB");
	ma_wb_.addAgentGroup("WB_");
	bs_bs.addAgentGroup("BS_BS");
	bs_wb.addAgentGroup("BS_WB");
	new MatsimEventsReader(events).readFile(eventsF);
	System.out.println("Mode stats all");
	for (Entry<String, MutableInt> e : ma_a.getTripsPerMode().entrySet() ){
		System.out.println(e.getKey()+"\t "+e.getValue().toString());
	}
	System.out.println("Mode stats WB_WB");
	for (Entry<String, MutableInt> e : ma_wb_wb.getTripsPerMode().entrySet() ){
		System.out.println(e.getKey()+"\t "+e.getValue().toString());
	}
	System.out.println("Mode stats WB_*");
	for (Entry<String, MutableInt> e : ma_wb_.getTripsPerMode().entrySet() ){
		System.out.println(e.getKey()+"\t "+e.getValue().toString());
	}
	System.out.println("Mode stats BS_BS");
	for (Entry<String, MutableInt> e : bs_bs.getTripsPerMode().entrySet() ){
		System.out.println(e.getKey()+"\t "+e.getValue().toString());
	}
	System.out.println("Mode stats BS_WB");
	for (Entry<String, MutableInt> e : bs_wb.getTripsPerMode().entrySet() ){
		System.out.println(e.getKey()+"\t "+e.getValue().toString());
	}
	
	
}
}
