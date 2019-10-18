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
package playground.vsp.analysis.modules.ptTripAnalysis.distance;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.ptTripAnalysis.AbstractAnalysisTrip;
import playground.vsp.analysis.modules.ptTripAnalysis.AbstractAnalysisTripSet;
import playground.vsp.analysis.modules.ptTripAnalysis.AbstractPlan2TripsFilter;
import playground.vsp.analysis.modules.ptTripAnalysis.AnalysisTripSetStorage;

/**
 * @author droeder
 * actually this class is compiling, but not running
 */
@Deprecated
public class DistanceAnalysis {
	//TODO [dr]debugging
	private static final Logger log = Logger.getLogger(DistanceAnalysis.class);
	
	private DistAnalysisHandler eventsHandler;

	private String unprocessedAgents;
	
	public DistanceAnalysis(){
		this.eventsHandler = new DistAnalysisHandler();
	}
	
	public void addZones(Map<String, Geometry> zones){
		this.eventsHandler.addZones(zones);
	}
	

	public void run(String plans, String network, String events, String outDir){
		this.readPlansAndNetwork(plans, network);
		log.info("streaming plans finished!");
		this.readEvents(events);
		log.info("streaming events finished!");
		this.write2csv(outDir);
		log.info("output written to " + outDir);
	}

	/**
	 * @param plans
	 * @param network
	 */
	@SuppressWarnings("unchecked")
	private void readPlansAndNetwork(String plans, String network) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(network);
		this.eventsHandler.addLinks((Map<Id<Link>, Link>) sc.getNetwork().getLinks());
		
//		final Population reader = (Population) sc.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( sc ) ;
		StreamingDeprecated.setIsStreaming(reader, true);
		AbstractPlan2TripsFilter planFilter = new DistPlan2TripsFilter();
		final PersonAlgorithm algo = planFilter; 
		reader.addAlgorithm(algo);
		
//		new MatsimPopulationReader(sc).parse(IOUtils.getInputStream(plans));
		reader.parse(IOUtils.getInputStream(IOUtils.getFileUrl(plans)));
		
		for(Entry<Id, LinkedList<AbstractAnalysisTrip>> e:  planFilter.getTrips().entrySet()){
			this.eventsHandler.addPerson(new DistAnalysisAgent(e.getValue(), e.getKey()));
		}
		this.unprocessedAgents = planFilter.getUnprocessedAgents();
	}
	
	/**
	 * @param events
	 */
	private void readEvents(String events) {
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this.eventsHandler);

		new EventsReaderXMLv1(manager).parse(IOUtils.getInputStream(IOUtils.getFileUrl(events)));
	}

	/**
	 * @param outDir
	 */
	private void write2csv(String out) {
		BufferedWriter writer;
		try {
			// write analysis
			for(Entry<String, AnalysisTripSetStorage> e : this.eventsHandler.getAnalysisTripSetStorage().entrySet()){
				for(Entry<String, AbstractAnalysisTripSet> o : e.getValue().getTripSets().entrySet()){
					writer = IOUtils.getBufferedWriter(out + e.getKey() + "_" + o.getKey() + "_trip_distance_analysis.csv");
					writer.write(o.getValue().toString());
					writer.flush();
					writer.close();
				}
			}
			
			//write stuckAgents
			writer = IOUtils.getBufferedWriter(out + "trip_distance_analysis_stuckAgents.csv");
			for(Id id : this.eventsHandler.getStuckAgents()) {
				writer.write(id.toString() + "\n");
			}
			writer.flush();
			writer.close();
			
			//write routes
			boolean header = true;
			writer = IOUtils.getBufferedWriter(out + "trip_distance_analysis_routes.csv");
			for(DistAnalysisTransitRoute r: this.eventsHandler.getRoutes()){
				writer.write(r.toString(header));
				header = false;
			}
			writer.flush();
			writer.close();
			
			//write vehicles
			writer = IOUtils.getBufferedWriter(out + "trip_distance_analysis_vehicles.csv");
			header = true;
			for(DistAnalysisVehicle v : this.eventsHandler.getVehicles()){
				writer.write(v.toString(header));
				header = false;
			}
			writer.flush();
			writer.close();
			
			//write unprocessed Agents
			writer = IOUtils.getBufferedWriter(out + "trip_distance_analysis_uprocessed_Agents.csv");
			writer.write(this.unprocessedAgents);
			writer.flush();
			writer.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
