/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;

import playground.andreas.bvgAna.level3.AgentId2StopDifferenceMap;

/**
 * Collects all missed vehicles events and their delay for each stop
 *
 * @author aneumann
 *
 */
public class StopId2MissedVehMap implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler, AgentDepartureEventHandler, PersonEntersVehicleEventHandler{

	private final Logger log = Logger.getLogger(StopId2MissedVehMap.class);
	private final Level logLevel = Level.DEBUG;

	private AgentId2StopDifferenceMap agentId2StopDifferenceMap;
	private TreeMap<Id, StopId2MissedVehMapData> stopId2StopId2MissedVehMapDataMap = null;

	public StopId2MissedVehMap(Population pop){
		this.log.setLevel(this.logLevel);
		Set<Id> agentIds = new TreeSet<Id>(pop.getPersons().keySet());
		this.agentId2StopDifferenceMap = new AgentId2StopDifferenceMap(pop, agentIds);
	}

	/**
	 * @return A map containing a <code>StopId2MissedVehMapData</code> for each stop.
	 */
	public TreeMap<Id, StopId2MissedVehMapData> getStopId2StopId2MissedVehMapDataMap() {
		if(this.stopId2StopId2MissedVehMapDataMap == null){
			this.stopId2StopId2MissedVehMapDataMap = new TreeMap<Id, StopId2MissedVehMapData>();
			calculateStopId2MissedVehMap();
			calculateStopId2AverageDelayMap();
		}

		return this.stopId2StopId2MissedVehMapDataMap;
	}

	private void calculateStopId2AverageDelayMap() {
		Map<Id, List<Tuple<Id, Double>>> delayMap = this.agentId2StopDifferenceMap.getAgentId2StopDifferenceMap();

		for (List<Tuple<Id, Double>> delayList : delayMap.values()) {
			for (Tuple<Id, Double> delayTuple : delayList) {
				if(this.stopId2StopId2MissedVehMapDataMap.get(delayTuple.getFirst()) == null){
					this.stopId2StopId2MissedVehMapDataMap.put(delayTuple.getFirst(), new StopId2MissedVehMapData(delayTuple.getFirst()));
				}

				this.stopId2StopId2MissedVehMapDataMap.get(delayTuple.getFirst()).addDelay(delayTuple.getSecond());
			}
		}

	}

	private void calculateStopId2MissedVehMap() {
		Map<Id, List<Tuple<Id, Integer>>> numberOfMissedVehiclesMap = this.agentId2StopDifferenceMap.getNumberOfMissedVehiclesMap();

		for (List<Tuple<Id, Integer>> stopId2MissedVehiclesList : numberOfMissedVehiclesMap.values()) {
			for (Tuple<Id, Integer> stopId2MissedVehicleTuple : stopId2MissedVehiclesList) {
				if(this.stopId2StopId2MissedVehMapDataMap.get(stopId2MissedVehicleTuple.getFirst()) == null){
					this.stopId2StopId2MissedVehMapDataMap.put(stopId2MissedVehicleTuple.getFirst(), new StopId2MissedVehMapData(stopId2MissedVehicleTuple.getFirst()));
				}

				this.stopId2StopId2MissedVehMapDataMap.get(stopId2MissedVehicleTuple.getFirst()).addMissedVehicle(stopId2MissedVehicleTuple.getSecond());
			}
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.agentId2StopDifferenceMap.handleEvent(event);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.agentId2StopDifferenceMap.handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.agentId2StopDifferenceMap.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.agentId2StopDifferenceMap.handleEvent(event);
	}
	
	public void writeResultsToFile(String filename){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
			
			TreeMap<Id, StopId2MissedVehMapData> results = this.getStopId2StopId2MissedVehMapDataMap();
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
