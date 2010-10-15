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

package playground.andreas.bvgAna.plan2DepartureTime;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * @author aneumann
 *
 */
public class Plan2PlannedDepartureTime {
	
	private static final Logger log = Logger.getLogger(Plan2PlannedDepartureTime.class);
	private static final Level logLevel = Level.DEBUG;
	
	/**
	 * Returns the planned departure time for each pt leg of a given set of agents
	 * 
	 * @param pop The population
	 * @param agentIds The Set of agents to be analyzed
	 * @return A map sorted first by agentId, second by StopId-Time tuples
	 */
	public static TreeMap<Id,ArrayList<Tuple<Id,Double>>> getPlannedDepartureTimeForPlan(Population pop, Set<Id> agentIds){
		
		// copied from ExperimentalTransitRoute
		final String SEPARATOR = "===";
		final String IDENTIFIER_1 = "PT1" + SEPARATOR;
		
		Plan2PlannedDepartureTime.log.setLevel(Plan2PlannedDepartureTime.logLevel);
		
		TreeMap<Id, ArrayList<Tuple<Id, Double>>> agentId2PlannedDepartureMap = new TreeMap<Id, ArrayList<Tuple<Id,Double>>>();
		
		for (Person person : pop.getPersons().values()) {
			if(agentIds.contains(person.getId())){
				
				// person in set, so do something
				
				ArrayList<Tuple<Id, Double>> plannedDepartureList = new ArrayList<Tuple<Id,Double>>();
				agentId2PlannedDepartureMap.put(person.getId(), plannedDepartureList);
				
				Plan plan = person.getSelectedPlan();
				double runningTime = 0.0;
				boolean firstActDone = false;
				for (PlanElement pE : plan.getPlanElements()) {
					
					if(pE instanceof ActivityImpl){
						ActivityImpl act = (ActivityImpl) pE;					

						if(!firstActDone){
							runningTime = act.getEndTime();
							firstActDone = true;
						} else {
							if(act.getDuration() != Time.UNDEFINED_TIME){
								runningTime += act.getDuration();
							} else {
								runningTime = act.getEndTime();
							}
						}
					}
					
					if(pE instanceof Leg){
						
						Leg leg = (Leg) pE;
						
						if(leg.getMode() == TransportMode.pt){
							// it's the start of a new pt leg, report it
							if (leg.getRoute() instanceof GenericRouteImpl){

								String routeDescription = ((GenericRouteImpl) leg.getRoute()).getRouteDescription();
								if (routeDescription.startsWith(IDENTIFIER_1)) {
									String[] parts = routeDescription.split(SEPARATOR, 6);//StringUtils.explode(routeDescription, '\t', 6);
									Id accessStopId = new IdImpl(parts[1]);
//									Id lineId = new IdImpl(parts[2]);
//									Id routeId = new IdImpl(parts[3]);
//									Id egressStopId = new IdImpl(parts[4]);
									
									plannedDepartureList.add(new Tuple<Id, Double>(accessStopId, new Double(runningTime)));
								}
								
							} else {
								log.warn("unknown route descriton found - only know to handle GenericRouteImpl");
							}
							
						}
						
						// add the legs travel time
						if(Double.isInfinite(leg.getTravelTime())){
							log.debug("Infinite travel time founde");
						} else {
							runningTime += leg.getTravelTime();
						}
						
					}					
				}				
			}
		}
		return agentId2PlannedDepartureMap;
	}

}
