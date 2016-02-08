/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.santiago;

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.analysis.legMode.distributions.FilteredDepartureTimeAnalyzer;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class SantiagoAnalyses {

	private static final String RUN_DIR = "../../../../repos/runs-svn/santiago/output/";
	private static final String [] RUN_CASES = {"ctd","outerCordon","triangleCordon"}; 

	public static void main(String[] args) {
		SantiagoAnalyses sa = new SantiagoAnalyses();
		sa.writeModalShare();
		sa.writeDepartureCounts();
		sa.writeModalTravelTimes();
		sa.writeDelay();
	}
	
	public void writeDelay(){
		for(String rc :RUN_CASES) { // here leg counts do not include legs which are stuck at some point of time.
			String eventsFile = RUN_DIR+rc+"/output_events.xml.gz";
			String outputFile = RUN_DIR+"/analysis/delay_"+rc+".txt";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(RUN_DIR+rc+"/output_plans.xml.gz", RUN_DIR+rc+"/output_network.xml.gz");
			ExperiencedDelayAnalyzer eda = new ExperiencedDelayAnalyzer(eventsFile, sc, 60, 30*3600);//half hour timebin		
			eda.run();
			eda.writeResults(outputFile);
		}
	}
	
	public void writeModalTravelTimes(){
		// I could sort it based on zone (outside/inside cordon) but, a trip may start inside and end outside or vice versa, 
		// thus depends how would you define it. 
		for(String rc :RUN_CASES) { // here leg counts do not include legs which are stuck at some point of time.
			String eventsFile = RUN_DIR+rc+"/output_events.xml.gz";
			String outputFile = RUN_DIR+"/analysis/modalTravelTimes_"+rc+".txt";
			ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile);		
			mtta.run();
			mtta.writeResults(outputFile);
		}
	}
	
	public void writeDepartureCounts(){
		double timebinSize = 1800; // half hour time bin is used, because cordon pricing starts at 7:30
		for(String rc :RUN_CASES) {
			String eventsFile = RUN_DIR+rc+"/output_events.xml.gz";
			String outputFile = RUN_DIR+"/analysis/modalDepartureCounts_"+rc+".txt";
			FilteredDepartureTimeAnalyzer fdta = new FilteredDepartureTimeAnalyzer(eventsFile, timebinSize);		
			fdta.run();
			fdta.writeResults(outputFile);
		}
	}

	public void writeModalShare(){
		for(String rc :RUN_CASES) {
			String eventsFile = RUN_DIR+rc+"/output_events.xml.gz";
			String outputFile = RUN_DIR+"/analysis/modalSplit_"+rc+".txt";
			ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
			msc.run();
			msc.writeResults(outputFile);
		}
	}
}