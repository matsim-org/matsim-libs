/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoSim.java
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
package playground.johannes.coopsim.pysical;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 * 
 */
public class PseudoSim {

	private static final Logger logger = Logger.getLogger(PseudoSim.class);

	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	public void run(Collection<Plan> plans, Network network, TravelTime linkTravelTimes, EventsManager eventManager) {
		Queue<Event> eventQueue = new LinkedList<Event>();

		logger.debug("Creating events...");

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
					LinkNetworkRoute route = (LinkNetworkRoute) ((Leg) elements.get(idx - 1)).getRoute();
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
					AgentArrivalEvent arrivalEvent = new AgentArrivalEventImpl(arrivalTime, plan.getPerson().getId(),
							act.getLinkId(), TransportMode.car);
					eventQueue.add(arrivalEvent);
					ActivityEvent startEvent = new ActivityStartEventImpl(arrivalTime, plan.getPerson().getId(),
							act.getLinkId(), act.getFacilityId(), act.getType());
					eventQueue.add(startEvent);
				}

				if (idx < elements.size() - 1) {
					/*
					 * This is not the last activity, send activity end and
					 * departure events.
					 */
					ActivityEvent endEvent = new ActivityEndEventImpl(actEndTime, plan.getPerson().getId(),
							act.getLinkId(), act.getFacilityId(), act.getType());
					eventQueue.add(endEvent);
					AgentDepartureEvent deparutreEvent = new AgentDepartureEventImpl(actEndTime, plan.getPerson()
							.getId(), act.getLinkId(), TransportMode.car);
					eventQueue.add(deparutreEvent);
				}

				prevEndTime = actEndTime;
			}
		}

		logger.debug("Processing events...");

		double lastTime = 0;
		for (Event event : eventQueue) {
			if ((event.getTime() % 3600 == 0) && (event.getTime() > lastTime)) {
				logger.debug(String.format("Pseude simulation at %1$sh.", event.getTime() / 3600));
				lastTime = event.getTime();
			}
			eventManager.processEvent(event);
		}
	}

	private double calcRouteTravelTime(LinkNetworkRoute route, double startTime, TravelTime travelTime, Network network) {
		double tt = 0;
		if (route.getStartLinkId() != route.getEndLinkId()) {

			List<Id> ids = route.getLinkIds();
			for (int i = 0; i < ids.size(); i++) {
				tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime);
				tt++;// 1 sec for each node
			}
			tt += travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), startTime);
		}

		return tt;
	}
}
