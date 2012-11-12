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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.bvgAna.anaLevel2.agentId2stopDifference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.bvgAna.anaLevel0.AgentId2PlannedDepartureTimeMap;
import playground.vsp.analysis.modules.bvgAna.anaLevel0.AgentId2PlannedDepartureTimeMapData;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.PersonId2DelayAtStopHandler;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.StopId2RouteId2DelayAtStopHandler;

/**
 * 
 * @author ikaddoura, andreas
 *
 */
public class AgentId2StopDifferenceAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(AgentId2StopDifferenceAnalyzer.class);
	private ScenarioImpl scenario;
	private StopId2RouteId2DelayAtStopHandler stopDelayHandler;
	private VehicleDeparturesAnalysis vehDepartures;
	private PersonId2DelayAtStopHandler personDelayHandler;
	private Map<Id, List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>>> plannedDepartureTimeMap;
	private AgentId2StopDifferenceAnalysis agentId2StopDifference;
	private TreeMap<Id, StopId2MissedVehMapData> stopId2StopId2MissedVehData;
	
	public AgentId2StopDifferenceAnalyzer(String ptDriverPrefix) {
		super(AgentId2StopDifferenceAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.stopDelayHandler = new StopId2RouteId2DelayAtStopHandler();
		this.personDelayHandler = new PersonId2DelayAtStopHandler(ptDriverPrefix);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.stopDelayHandler);
		handler.add(this.personDelayHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		this.plannedDepartureTimeMap = AgentId2PlannedDepartureTimeMap.getAgentId2PlannedPTDepartureTimeMap(this.scenario.getPopulation());
	}

	@Override
	public void postProcessData() {
		this.vehDepartures = new VehicleDeparturesAnalysis(this.stopDelayHandler);
		this.agentId2StopDifference = new AgentId2StopDifferenceAnalysis(this.scenario.getPopulation(), plannedDepartureTimeMap, vehDepartures, personDelayHandler);
		
		// get results	
		StopId2MissedVehMap stopId2MissedVehAna = new StopId2MissedVehMap(this.agentId2StopDifference);
		this.stopId2StopId2MissedVehData = stopId2MissedVehAna.getStopId2StopId2MissedVehMapDataMap();
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "missedVehicles.txt";
		File file = new File(fileName);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			
			TreeMap<Id, StopId2MissedVehMapData> results = this.stopId2StopId2MissedVehData;
			for (StopId2MissedVehMapData data : results.values()) {
				writer.write(data.getFullStatistics()); writer.newLine();
			}
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
