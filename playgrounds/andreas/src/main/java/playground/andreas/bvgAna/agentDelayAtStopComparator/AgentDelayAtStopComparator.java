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

package playground.andreas.bvgAna.agentDelayAtStopComparator;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;

import playground.andreas.bvgAna.agentDelayAnalyzer.AgentDelayHandler;
import playground.andreas.bvgAna.plan2DepartureTime.Plan2PlannedDepartureTime;
import playground.andreas.bvgAna.vehDelayHandler.VehDelayAnalyzer;
import playground.andreas.bvgAna.vehDelayHandler.VehDelayHandler;

/**
 * Calculates the difference between realized time and planned time spent waiting at a stop for a given set of agent ids.
 * Returns negative values for agents being faster than planned.
 * Returns positive values for agents being slower than planned.
 * 
 * @author aneumann
 *
 */
public class AgentDelayAtStopComparator implements VehicleDepartsAtFacilityEventHandler, AgentDepartureEventHandler, PersonEntersVehicleEventHandler{

	private final Logger log = Logger.getLogger(AgentDelayAtStopComparator.class);
	private final Level logLevel = Level.DEBUG;
	
	private Population pop;
	private Set<Id> agentIds;	
	private TreeMap<Id, ArrayList<Tuple<Id, Double>>> plannedDepartureTimeMap;
	private VehDelayHandler vehDelayHandler;
	private VehDelayAnalyzer vehDelayAnalyzer;
	private AgentDelayHandler agentDelayHandler;	
	private TreeMap<Id,ArrayList<Tuple<Id,Double>>> agentIds2StopDifferenceMap = null;

	public AgentDelayAtStopComparator(Population pop, Set<Id> agentIds){
		this.log.setLevel(this.logLevel);
		this.pop = pop;
		this.agentIds = agentIds;
		
		this.vehDelayHandler = new VehDelayHandler();
		this.vehDelayAnalyzer = new VehDelayAnalyzer(this.vehDelayHandler);
		this.agentDelayHandler = new AgentDelayHandler(agentIds);
		
		this.log.info("Reading planned departure time...");
		this.plannedDepartureTimeMap = Plan2PlannedDepartureTime.getPlannedDepartureTimeForPlan(this.pop, this.agentIds);
	}	
	
	private void compare(){		
		this.agentIds2StopDifferenceMap = new TreeMap<Id,ArrayList<Tuple<Id,Double>>>();

		for (Id agentId : this.agentIds) {

			if(this.agentIds2StopDifferenceMap.get(agentId) == null){
				this.agentIds2StopDifferenceMap.put(agentId, new ArrayList<Tuple<Id,Double>>());
			}

			ArrayList<Tuple<Id,Double>> agentsDiffs = this.agentIds2StopDifferenceMap.get(agentId);				
			ArrayList<Tuple<Id, Double>> plannedDepartures = this.plannedDepartureTimeMap.get(agentId);

			for (int i = 0; i < plannedDepartures.size(); i++) {

				Id stopId = plannedDepartures.get(i).getFirst();				
				double plannedDepartureTime = plannedDepartures.get(i).getSecond().doubleValue();

				// get next possible departure as scheduled
				double nextPlannedVehDeparture = this.vehDelayAnalyzer.getNextPlannedDepartureTime(stopId, plannedDepartureTime);

				// calculate resulting time spent waiting
				double plannedTimeWaiting = nextPlannedVehDeparture - plannedDepartureTime;

				// get realized values
				double realizedDepartureTime = this.agentDelayHandler.getStopId2DelayAtStopMap().get(agentId).getAgentDepartsPTInteraction().get(i).doubleValue();
				double realizedVehEntersTime = this.agentDelayHandler.getStopId2DelayAtStopMap().get(agentId).getAgentEntersVehicle().get(i).doubleValue();

				// calculate resulting time spent waiting
				this.log.debug("Realized time waiting at the stop is counted by calculating \"agent enters vehicle\" minus \"agent departs pt interaction\"." +
						"Maybe this should be done by taking the corresponding VehicleDepartsAtFacilityEvent instead of the PersonEntersVehicleEvent.");
				double realizedTimeWaiting = realizedVehEntersTime - realizedDepartureTime; 

				// calculate difference, so that agents faster than planned get negative values and slower agents positive values
				double difference = realizedTimeWaiting - plannedTimeWaiting;

				// put the resulting difference in the map
				agentsDiffs.add(new Tuple<Id, Double>(stopId, new Double(difference)));
			}				
		}		
	}
	
	/**
	 * Returns resulting difference between planned and realized time spent waiting at each stop
	 * 
	 * @return A map containing a list of the resulting difference for each agent
	 */
	public TreeMap<Id, ArrayList<Tuple<Id, Double>>> getDifferenceMap(){
		if(this.agentIds2StopDifferenceMap == null){
			compare();
		}
		return this.agentIds2StopDifferenceMap;
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehDelayHandler.handleEvent(event);		
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");		
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.agentDelayHandler.handleEvent(event);		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.agentDelayHandler.handleEvent(event);		
	}
	
}
