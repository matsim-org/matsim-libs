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

package playground.vsp.analysis.modules.bvgAna.anaLevel2.agentId2stopDifference;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

/**
 * Collects all missed vehicles events and their delay for each stop
 *
 * @author aneumann
 *
 */
public class StopId2MissedVehMap {

	private final Logger log = Logger.getLogger(StopId2MissedVehMap.class);
	private final Level logLevel = Level.DEBUG;

	private AgentId2StopDifferenceAnalysis agentId2StopDifferenceMap;
	private TreeMap<Id, StopId2MissedVehMapData> stopId2StopId2MissedVehMapDataMap = null;

	public StopId2MissedVehMap(AgentId2StopDifferenceAnalysis agentId2StopDifferenceMap){
		this.log.setLevel(this.logLevel);
		this.agentId2StopDifferenceMap = agentId2StopDifferenceMap;
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
	
}
