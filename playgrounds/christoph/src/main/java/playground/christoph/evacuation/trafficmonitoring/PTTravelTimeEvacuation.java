/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeEvacuation.java
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

package playground.christoph.evacuation.trafficmonitoring;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.InformedAgentsTracker;
import playground.christoph.evacuation.pt.EvacuationTransitRouter;
import playground.christoph.evacuation.pt.EvacuationTransitRouterFactory;

public class PTTravelTimeEvacuation implements SwissPTTravelTime {

	private final static Logger log = Logger.getLogger(PTTravelTimeEvacuation.class);
	
	/*
	 * Before the evacuation has started, agents use another travel disutility
	 * function than after the evacuation has started.
	 */
//	private final TransitRouter router;	// before evacuation has started
	private final EvacuationTransitRouterFactory evacuationRouterFactory;	// after evacuation has started
	private final Map<Id, Double> agentSpeedMap;
	private final TravelTime ptTravelTime;
	private final InformedAgentsTracker informedAgentsTracker;
	
	private final ThreadLocal<EvacuationTransitRouter> evacuationRouters;
	private final ThreadLocal<Double> personSpeed;
	
	/*
	 * So far we use/need a simple travel time object to calculate pt travel times
	 * for trips that are performed before the evacuation has started.
	 */
	public PTTravelTimeEvacuation(EvacuationTransitRouterFactory evacuationRouterFactory,
			Map<Id, Double> agentSpeedMap, TravelTime ptTravelTime, InformedAgentsTracker informedAgentsTracker) {
		this.evacuationRouterFactory = evacuationRouterFactory;
		this.agentSpeedMap = agentSpeedMap;
		this.ptTravelTime = ptTravelTime;
		this.informedAgentsTracker = informedAgentsTracker;
		
		this.evacuationRouters = new ThreadLocal<EvacuationTransitRouter>();
		this.personSpeed = new ThreadLocal<Double>();
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		setPerson(person);
		if (this.personSpeed.get() != null) {
			return link.getLength() / this.personSpeed.get();
		} else return ptTravelTime.getLinkTravelTime(link, time, person, vehicle);
	}

	private void setPerson(Person person) {
		/*
		 * If no speed value for the person is set in the map, the 
		 * agent performs the PT trip before the evacuation starts. 
		 * Therefore use the simple PT travel time calculator, which
		 * is done when personSpeed contains a null value.
		 */
		Double personSpeed = agentSpeedMap.get(person.getId());
		this.personSpeed.set(personSpeed);	
	}

	@Override
	public void setPersonSpeed(Id personId, double speed) {
		this.agentSpeedMap.put(personId, speed);
	}

	@Override
	public Tuple<Double, Coord> calcSwissPtTravelTime(final Activity fromAct, final Activity toAct, final double depTime, Person person) {

		boolean isInformed = informedAgentsTracker.isAgentInformed(person.getId());
		double travelTime = Double.MAX_VALUE;
		Coord exitCoord = null;

		EvacuationTransitRouter evacuationRouter = this.evacuationRouters.get();
		if (evacuationRouter == null) {
			evacuationRouter = this.evacuationRouterFactory.createTransitRouter();
			this.evacuationRouters.set(evacuationRouter);
		}
		
		if (!isInformed) {
//			Path path = evacuationRouter.calcExitPath(fromAct.getCoord(), depTime, person);
			Path path = evacuationRouter.calcPath(fromAct.getCoord(), toAct.getCoord(), depTime, person);
			if (path != null) {
				travelTime = path.travelTime;
				exitCoord = path.nodes.get(path.nodes.size() - 1).getCoord();
					
				/*
				 * The person is not informed yet, but still the evacuation might have been started.
				 * Therefore, a penalty might be added to the travel time since the public transport
				 * availability might be reduced.
				 */
				if (depTime >= EvacuationConfig.evacuationTime && travelTime < Double.MAX_VALUE) {
					double penaltyFactor = EvacuationConfig.ptTravelTimePenaltyFactor;
					travelTime *= penaltyFactor;
				}
			} 
			// no pt route was found was found
			else {
				travelTime = Double.MAX_VALUE;
				exitCoord = null;
			}
		}
		/*
		 * agent is informed
		 */
		else {
			/*
			 * route to rescue facility
			 */
			if (toAct.getFacilityId().toString().equals("rescueFacility")) {
				Path exitPath = evacuationRouter.calcExitPath(fromAct.getCoord(), depTime, person);
				if (exitPath != null) {
					travelTime = exitPath.travelTime;
					exitCoord = exitPath.nodes.get(exitPath.nodes.size() - 1).getCoord();
					
					if (travelTime < Double.MAX_VALUE) {
						// the person has been informed, therefore also the evacuation has already started
						double penaltyFactor = EvacuationConfig.ptTravelTimePenaltyFactor;
						travelTime *= penaltyFactor;
					}
				}
				// no pt route was found was found
				else {
					travelTime = Double.MAX_VALUE;
					exitCoord = null;
				}
			}
			/*
			 * other route
			 */
			else {
//				Path path = evacuationRouter.calcExitPath(fromAct.getCoord(), depTime, person);
				Path path = evacuationRouter.calcPath(fromAct.getCoord(), toAct.getCoord(), depTime, person);
				if (path != null) {
					travelTime = path.travelTime;
					exitCoord = path.nodes.get(path.nodes.size() - 1).getCoord();

					if (travelTime < Double.MAX_VALUE) {
						// the person has been informed, therefore also the evacuation has already started
						double penaltyFactor = EvacuationConfig.ptTravelTimePenaltyFactor;
						travelTime *= penaltyFactor;
					}
				}
				// no pt route was found was found
				else {
					travelTime = Double.MAX_VALUE;
					exitCoord = null;
				}
			}
		}
		
		if (travelTime == Double.MAX_VALUE) {
			log.warn("No transit route was found for agent " + person.getId().toString() + 
					" from link " + fromAct.getLinkId().toString() + 
					" to link " + toAct.getLinkId().toString());
		} else if (travelTime == 0.0) {
			double distance = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
			log.warn("Found travel time of 0.0 seconds for a crow fly distance of " + distance +
					". Time: " + depTime + ", person: " + person.getId());
		}
		return new Tuple<Double, Coord>(travelTime, exitCoord);
	}
	
}