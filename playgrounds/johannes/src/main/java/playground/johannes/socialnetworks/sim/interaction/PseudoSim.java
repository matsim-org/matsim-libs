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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 * 
 */
public class PseudoSim {

	private static final Logger logger = Logger.getLogger(PseudoSim.class);

	private Queue<Event> eventQueue;

	private Network network;

	private final Comparator<? super Event> comparator = new Comparator<Event>() {

		@Override
		public int compare(Event o1, Event o2) {
			double r = o1.getTime() - o2.getTime();
			if (r > 0)
				return 1;
			if (r < 0)
				return -1;
			else {
				if (o1 == o2)
					return 0;
				else {
					int rank = rank(o1) - rank(o2);
					if(rank == 0)
						return o1.hashCode() - o2.hashCode();
					else
						return rank;
				}
			}
		}
		
		private int rank(Event event) {
			if(event instanceof ActivityEndEvent)
				return 1;
			else if(event instanceof AgentDepartureEvent)
				return 2;
			else if(event instanceof AgentArrivalEvent)
				return 3;
			else if(event instanceof ActivityStartEvent)
				return 4;
			else
				return 5;
		}
	};

	public void run(Population population, Network network, TravelTime travelTime, EventsManagerImpl eventManager) {
		this.network = network;

		eventManager.resetHandlers(0);
		eventQueue = new PriorityQueue<Event>(population.getPersons().size(), comparator);

		logger.info("Creating events...");

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<PlanElement> elements = plan.getPlanElements();

			boolean carMode = true;
			for (int i = 1; i < elements.size(); i += 2) {
				if (!((Leg) elements.get(i)).getMode().equalsIgnoreCase("car")) {
					carMode = false;
					break;
				}
			}

			if (carMode) {
				double lastEndTime = 0;
				for (int i = 0; i < elements.size(); i += 2) {
					Activity act = (Activity) elements.get(i);
					double startTime = 0.0;
					double endTime = act.getEndTime();

					if(Double.isInfinite(endTime)) {
						endTime = Math.max(lastEndTime + 1, 86400);
					}
					
					if (i > 0) {
						LinkNetworkRoute route = (LinkNetworkRoute) ((Leg) elements.get(i - 1)).getRoute();
						double tt = calcRouteTravelTime(route, lastEndTime, travelTime);
						tt = Math.max(1, tt);
						startTime = tt + lastEndTime;
					}
					
					endTime = Math.max(startTime + 1, endTime);
					
					if(i > 0) {
						AgentArrivalEvent arrivalEvent = new AgentArrivalEventImpl(startTime, person.getId(), act.getLinkId(), TransportMode.car);
						eventQueue.add(arrivalEvent);
					}
					ActivityEvent startEvent = new ActivityStartEventImpl(startTime, person.getId(), act.getLinkId(),
							act.getFacilityId(), act.getType());
					
					ActivityEvent endEvent = new ActivityEndEventImpl(endTime, person.getId(), act.getLinkId(), act
							.getFacilityId(), act.getType());
					
					eventQueue.add(startEvent);
					act.setStartTime(startEvent.getTime());
					eventQueue.add(endEvent);
					act.setEndTime(endEvent.getTime());

					
					if(i < elements.size()-1) {
						AgentDepartureEvent deparutreEvent = new AgentDepartureEventImpl(endTime, person.getId(), act.getLinkId(), TransportMode.car);
						eventQueue.add(deparutreEvent);
					}

					lastEndTime = endTime;
				}
			}
		}

		logger.info("Processing events...");
		
		Event event;
		while((event = eventQueue.poll()) != null) {
			eventManager.processEvent(event);
		}
	}

	private double calcRouteTravelTime(LinkNetworkRoute route, double startTime, TravelTime travelTime) {
		double tt = 0;
		List<Id> ids = route.getLinkIds();
		for (int i = 1; i < ids.size(); i++) {
			tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime);
		}
		return Math.max(tt, 1.0);
	}
}
