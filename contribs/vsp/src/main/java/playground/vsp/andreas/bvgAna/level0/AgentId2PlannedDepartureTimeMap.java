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

package playground.vsp.andreas.bvgAna.level0;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.routes.TransitPassengerRoute;

/**
 *
 * @author aneumann
 *
 */
public class AgentId2PlannedDepartureTimeMap {

	private static final Logger log = LogManager.getLogger(AgentId2PlannedDepartureTimeMap.class);
//	private static final Level logLevel = Level.DEBUG;

	/**
	 * Returns the planned departure time for each pt leg of a given set of agents
	 *
	 * @param pop The population
	 * @param agentIds The Set of agents to be analyzed
	 * @return A map sorted first by agentId, second by StopId-Time tuples
	 */
	public static Map<Id,List<Tuple<Id,AgentId2PlannedDepartureTimeMapData>>> getAgentId2PlannedPTDepartureTimeMap(Population pop, Set<Id<Person>> agentIds){

//		AgentId2PlannedDepartureTimeMap.log.setLevel(AgentId2PlannedDepartureTimeMap.logLevel);
		Map<Id, List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>>> agentId2PlannedDepartureMap = new TreeMap<Id, List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>>>();

		for (Person person : pop.getPersons().values()) {
			if(agentIds.contains(person.getId())){

				// person in set, so do something

				List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>> plannedDepartureList = new ArrayList<Tuple<Id, AgentId2PlannedDepartureTimeMapData>>();
				agentId2PlannedDepartureMap.put(person.getId(), plannedDepartureList);

				Plan plan = person.getSelectedPlan();
				double runningTime = 0.0;
				boolean firstActDone = false;
				for (PlanElement pE : plan.getPlanElements()) {

					if(pE instanceof Activity){
						Activity act = (Activity) pE;

						if(!firstActDone){
							runningTime = act.getEndTime().seconds();
							firstActDone = true;
						} else {
							if(act.getMaximumDuration().isUndefined()){
								runningTime += act.getMaximumDuration().seconds();
							} else {
								runningTime = act.getEndTime().seconds();
							}
						}
					}

					if(pE instanceof Leg){

						Leg leg = (Leg) pE;

						if(leg.getMode() == TransportMode.pt){
							// it's the start of a new pt leg, report it
							if (leg.getRoute() instanceof TransitPassengerRoute){
								TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
								plannedDepartureList.add(new Tuple<Id, AgentId2PlannedDepartureTimeMapData>(route.getAccessStopId(), new AgentId2PlannedDepartureTimeMapData(route.getAccessStopId(), runningTime, route.getLineId(), route.getRouteId())));
							} else if (leg.getRoute() != null) {
								log.warn("unknown route description found - only know to handle ExperimentalTransitRoute, got " + leg.getRoute().getClass().getCanonicalName());
							}
						}

						// add the legs travel time
						if(leg.getTravelTime().isUndefined()){
							log.debug("Undefined travel time found");
						} else {
							runningTime += leg.getTravelTime().seconds();
						}

					}
				}
			}
		}
		return agentId2PlannedDepartureMap;
	}

}
