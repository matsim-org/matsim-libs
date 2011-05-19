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
package playground.droeder.Analysis.Trips.travelTime.V4;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.droeder.Analysis.Trips.AbstractAnalysisTripSet;
import playground.droeder.Analysis.Trips.AbstractPlan2TripsFilter;
import playground.droeder.Analysis.Trips.AnalysisTripSetStorage;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
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
	
	public void run(String plans, String network, String events, String outDir){
		this.readPlans(plans, network);
		log.info("streaming plans finished!");
		this.readEvents(events);
		log.info("streaming events finished!");
		this.write2csv(outDir);
		log.info("output written to " + outDir);
	}
	
	private void write2csv(String out){
		BufferedWriter writer;
		try {
			// write analysis
			for(Entry<String, AnalysisTripSetStorage> e : this.eventsHandler.getZone2Tripset().entrySet()){
				for(Entry<String, AbstractAnalysisTripSet> o : e.getValue().getTripSets().entrySet()){
					writer = IOUtils.getBufferedWriter(out + e.getKey() + "_" + o.getKey() + "_trip_analysis_v4.csv");
					writer.write(o.getValue().toString());
					writer.flush();
					writer.close();
				}
			}
			
			//write unprocessed Agents
			writer = IOUtils.getBufferedWriter(out + "unprocessedAgents_v4.csv");
			writer.write(this.unProcessedAgents);
			writer.flush();
			writer.close();
			
			//write uncompletedPlans
			writer = IOUtils.getBufferedWriter(out + "uncompletedPlans_v4.csv");
			writer.write(this.eventsHandler.getUncompletedPlans());
			writer.flush();
			writer.close();
			
			//write stuckAgents
			writer = IOUtils.getBufferedWriter(out + "stuckAgents_v4.csv");
			writer.write(this.eventsHandler.getStuckAgents());
			writer.flush();
			writer.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readPlans(String plans, String network){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc).parse(network);
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		AbstractPlan2TripsFilter planFilter = new Plan2TripsFilterV4(); 
		((PopulationImpl) sc.getPopulation()).addAlgorithm(planFilter);
		
		new MatsimPopulationReader(sc).parse(IOUtils.getInputstream(plans));
		
		this.unProcessedAgents = planFilter.getUnprocessedAgents();
		this.eventsHandler.addTrips( planFilter.getTrips());
	}
	
	private void readEvents(String eventsFile){
		
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this.eventsHandler);
		
		new EventsReaderXMLv1(manager).parse(IOUtils.getInputstream(eventsFile));
	}
}
