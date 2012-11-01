/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.andreas.aas.modules.ptTripAnalysis.traveltime.V4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.andreas.aas.modules.ptTripAnalysis.AbstractAnalysisTripSet;
import playground.andreas.aas.modules.ptTripAnalysis.AbstractPlan2TripsFilter;
import playground.andreas.aas.modules.ptTripAnalysis.AnalysisTripSetStorage;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author aneumann, droeder
 *
 */
public class TTtripAnalysisV4{
	private static final Logger log = Logger.getLogger(TTtripAnalysisV4.class);
	private TTtripEventsHandlerV4 eventsHandler;
	private String unProcessedAgents;
	
	public TTtripAnalysisV4 (){
		 this.eventsHandler = new TTtripEventsHandlerV4();
	}
	
	public void addZones(Map<String, Geometry> zones){
		this.eventsHandler.addZones(zones);
	}
	
	public void preProcessData(Population population) {
		AbstractPlan2TripsFilter planFilter = new Plan2TripsFilterV4();
		planFilter.run(population);
		this.unProcessedAgents = planFilter.getUnprocessedAgents();
		this.eventsHandler.addTrips( planFilter.getTrips());
		log.info("Processed all plans.");
	}
	
	public void writeResults(String outputFolder) {
		if(!new File(outputFolder).exists()){
			new File(outputFolder).mkdirs();
		}
		
		BufferedWriter writer;
		try {
			// write analysis
			for(Entry<String, AnalysisTripSetStorage> e : this.eventsHandler.getZone2Tripset().entrySet()){
				for(Entry<String, AbstractAnalysisTripSet> o : e.getValue().getTripSets().entrySet()){
					writer = IOUtils.getBufferedWriter(outputFolder + e.getKey() + "_" + o.getKey() + "_trip_analysis_v4.csv");
					writer.write(o.getValue().toString());
					writer.flush();
					writer.close();
				}
			}
			
			//write unprocessed Agents
			writer = IOUtils.getBufferedWriter(outputFolder + "unprocessedAgents_v4.csv");
			writer.write(this.unProcessedAgents);
			writer.flush();
			writer.close();
			
			//write uncompletedPlans
			writer = IOUtils.getBufferedWriter(outputFolder + "uncompletedPlans_v4.csv");
			writer.write(this.eventsHandler.getUncompletedPlans());
			writer.flush();
			writer.close();
			
			//write stuckAgents
			writer = IOUtils.getBufferedWriter(outputFolder + "stuckAgents_v4.csv");
			writer.write(this.eventsHandler.getStuckAgents());
			writer.flush();
			writer.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("Output successfully written.");
	}
	
	public EventHandler getEventHandler() {
		return this.eventsHandler;
	}
}
