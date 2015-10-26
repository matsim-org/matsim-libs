/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.services;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * @author illenberger
 *
 */
public class PseudoMobsim implements MobilitySimulation {
	
	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	private final TravelTime linkTravelTimes;
	
	private final Network network;
	
	public PseudoMobsim(Network network, TravelTime travelTimes) {
		this.linkTravelTimes = travelTimes;
		this.network = network;
	}
	
	@Override
	public void run(Collection<Plan> plans, EventsManager eventManager) {
		Queue<Event> eventQueue = new LinkedList<Event>();
		
		for (Plan plan : plans) {
			List<PlanElement> elements = plan.getPlanElements();

			double prevEndTime = 0;
			for (int idx = 0; idx < elements.size(); idx += 2) {
				Activity act = (Activity) elements.get(idx);
				/*
				 * Make sure that the activity does not end before the previous
				 * activity.
				 */
				double actEndTime = Math.max(prevEndTime + MIN_ACT_DURATION, act.getEndTime());

				if (idx > 0) {
					/*
					 * If this is not the first activity, then there must exist
					 * a leg before.
					 */
					NetworkRoute route = (NetworkRoute) ((Leg) elements.get(idx - 1)).getRoute();
					double travelTime = calcRouteTravelTime(route, prevEndTime, linkTravelTimes, network);
					travelTime = Math.max(MIN_LEG_DURATION, travelTime);
					double arrivalTime = travelTime + prevEndTime;
					/*
					 * If act end time is not specified...
					 */
					if (Double.isInfinite(actEndTime)) {
						throw new RuntimeException("I think this is discuraged.");
					}
					/*
					 * Make sure that the activity does not end before the agent
					 * arrives.
					 */
					actEndTime = Math.max(arrivalTime + MIN_ACT_DURATION, actEndTime);
					/*
					 * Send arrival and activity start events.
					 */
					PersonArrivalEvent arrivalEvent = new PersonArrivalEvent(arrivalTime, plan.getPerson().getId(),
							act.getLinkId(), TransportMode.car);
					eventQueue.add(arrivalEvent);
					ActivityStartEvent startEvent = new ActivityStartEvent(arrivalTime, plan.getPerson().getId(),
							act.getLinkId(), act.getFacilityId(), act.getType());
					eventQueue.add(startEvent);
				}

				if (idx < elements.size() - 1) {
					/*
					 * This is not the last activity, send activity end and
					 * departure events.
					 */
					ActivityEndEvent endEvent = new ActivityEndEvent(actEndTime, plan.getPerson().getId(),
							act.getLinkId(), act.getFacilityId(), act.getType());
					eventQueue.add(endEvent);
					PersonDepartureEvent deparutreEvent = new PersonDepartureEvent(actEndTime, plan.getPerson()
							.getId(), act.getLinkId(), TransportMode.car);
					eventQueue.add(deparutreEvent);
				}

				prevEndTime = actEndTime;
			}
		}

		for (Event event : eventQueue) {
			eventManager.processEvent(event);
		}
	}
	
	private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network) {
		double tt = 0;
		if (route.getStartLinkId() != route.getEndLinkId()) {

			List<Id<Link>> ids = route.getLinkIds();
			for (int i = 0; i < ids.size(); i++) {
				tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime, null, null);
				tt++;// 1 sec for each node
			}
			tt += travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), startTime, null, null);
		}

		return tt;
	}

}
