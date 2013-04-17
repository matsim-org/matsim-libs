/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.analysis.modules.ptTravelStats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @param args
 * @author sfuerbas
 */

public class travelStatsAnalyzer extends AbstractAnalyisModule {

	private final static Logger log = Logger.getLogger(travelStatsAnalyzer.class);
	private Scenario scenario;
	private travelStatsHandler handler;
	
	public travelStatsAnalyzer(Scenario scenario, Double interval) {
		super(travelStatsAnalyzer.class.getSimpleName());
		this.scenario = scenario;
		this.handler = new travelStatsHandler(scenario, interval);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
//		nothing to be pre-processed
	}

	@Override
	public void postProcessData() {
		
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "travelStats.txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.newLine();
			
			bw.write("");
			bw.newLine();
			
			for(Id linkId : this.scenario.getNetwork().getLinks().keySet()){
				Double bla = 0.;
				bw.write("");
				bw.write("");
				bw.newLine();
			}
			
			bw.close();

			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	





	
}
