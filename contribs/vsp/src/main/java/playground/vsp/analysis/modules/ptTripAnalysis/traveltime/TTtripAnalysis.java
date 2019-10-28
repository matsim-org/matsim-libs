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
package playground.vsp.analysis.modules.ptTripAnalysis.traveltime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptTripAnalysis.AbstractAnalysisTripSet;
import playground.vsp.analysis.modules.ptTripAnalysis.AbstractPlan2TripsFilter;
import playground.vsp.analysis.modules.ptTripAnalysis.AnalysisTripSetStorage;

/**
 * @author aneumann, droeder
 * 
 * This class analyzes the trip-dependent traveltime per mode 
 *
 */
public class TTtripAnalysis extends AbstractAnalysisModule{
	private static final Logger log = Logger.getLogger(TTtripAnalysis.class);
	private TTtripEventsHandler eventsHandler;
	private String unProcessedAgents;
	private Collection<String> networkmodes;
	private Collection<String> ptModes;
	private Population population;
	
	// currently, sep '13, for me (dr) this analysis does not work.
	// was trying to use it with an extended pt-tutorial, see 
	// playground.droeder.extendPtTutorial.ExtendPtTutorial#main (r25647)
	/**
	 * 
	 * @param ptModes, the modes specified in the {@link TransitConfigGroup}
	 * @param networkModes, the modes specified in the {@link PlansCalcRouteConfigGroup}
	 * @param ptDriverPrefix
	 * @param population, the population corresponding to the analyzed eventsfile (i.e. analyze runId.iteration.events.xml.gz, use 
	 * runId.iteration.plans.xml.gz!) 
	 */
	public TTtripAnalysis (Collection<String> ptModes, Collection<String> networkModes, Population population){
		super(TTtripAnalysis.class.getSimpleName());
		this.eventsHandler = new TTtripEventsHandler(ptModes);
		this.ptModes = ptModes;
		// not sure if this is necessary, but pt should be the default...
		this.networkmodes = networkModes;
		this.population = population;
	}
	
	public void addZones(Map<String, Geometry> zones){
		this.eventsHandler.addZones(zones);
	}
	
//	public void preProcessData(Population population) {
//		AbstractPlan2TripsFilter planFilter = new Plan2TripsFilterV4(this.ptModes, this.networkmodes);
//		planFilter.run(population);
//		this.unProcessedAgents = planFilter.getUnprocessedAgents();
//		this.eventsHandler.addTrips( planFilter.getTrips());
//		log.info("Processed all plans.");
//	}
	
	@Override
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
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.eventsHandler);
		return handler;
	}

	@Override
	public void preProcessData() {
		AbstractPlan2TripsFilter planFilter = new Plan2TripsFilter(this.ptModes, this.networkmodes);
		planFilter.run(this.population);
		this.unProcessedAgents = planFilter.getUnprocessedAgents();
		this.eventsHandler.addTrips( planFilter.getTrips());
		log.info("Processed all plans.");
	}

	@Override
	public void postProcessData() {
		
	}
}
