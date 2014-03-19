/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPseudoSim.java
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
package playground.johannes.gsv.sim;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.johannes.socialnetworks.utils.CollectionUtils;

/**
 * @author illenberger
 *
 */
public class ParallelPseudoSim {

	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	private SimThread[] threads;
	
	private Future<?>[] futures;
	
	private final ExecutorService executor;
	
	private final TransitSchedule schedule;
	
	private final Config config;
	
	public ParallelPseudoSim(int numThreads, Config config, TransitSchedule schedule) {
		this.config = config;
		this.schedule = schedule;
		
		executor = Executors.newFixedThreadPool(numThreads);
		
		threads = new SimThread[numThreads];
		for(int i = 0; i < numThreads; i++)
			threads[i] = new SimThread();
		
		futures = new Future[numThreads];
	}
	
//	public void init() {
//		this.plans = plans;
//		this.network = network;
//		this.linkTravelTimes = linkTravelTimes;
//		this.eventsManager = eventManager;
//	}
	
	public void run(Collection<Plan> plans, Network network, TravelTime linkTravelTimes, EventsManager eventManager) {
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(plans.size(), threads.length);
		List<Plan>[] segments = CollectionUtils.split(plans, n);
		/*
		 * submit tasks
		 */
		for(int i = 0; i < segments.length; i++) {
			threads[i].init(segments[i], network, linkTravelTimes, eventManager, config);
			futures[i] = executor.submit(threads[i]);
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < segments.length; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class SimThread implements Runnable {
		
		private Collection<Plan> plans;
		
		private TravelTime linkTravelTimes;
		
		private EventsManager eventManager;
		
		private Network network;
		
		private TransitRouterNetworkTravelTimeAndDisutility transitTravelTime;
		
		private PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(null);
		
		private Queue<Event> eventQueue;
		
		private RailSimEngine railSimEngine;
		
		public void init(Collection<Plan> plans, Network network, TravelTime linkTravelTimes, EventsManager eventManager, Config config) {
			this.plans = plans;
			this.network = network;
			this.linkTravelTimes = linkTravelTimes;
			this.eventManager = eventManager;
			TransitRouterConfig trConfig = new TransitRouterConfig(config);
			transitTravelTime = new TransitRouterNetworkTravelTimeAndDisutility(trConfig, preparedSchedule);
			
			
		}

		@Override
		public void run() {
			eventQueue = new LinkedList<Event>();
			railSimEngine = new RailSimEngine(eventQueue, schedule, network);
			
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
						Leg leg = (Leg) elements.get(idx - 1);
						double travelTime = Double.NaN;
						
						if(leg.getMode().equals(TransportMode.car)) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							travelTime = calcCarTravelTime(route, prevEndTime, linkTravelTimes, network);
						} else if (leg.getMode().equals(TransportMode.transit_walk)) {
							travelTime = calcTransitWalkTime(plan.getPerson(), (Activity)elements.get(idx - 2), act);
						} else if (leg.getMode().equals(TransportMode.pt)) {
//							ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
							travelTime = calcPTTravelTime(leg, actEndTime, plan.getPerson());
						}
						
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
								act.getLinkId(), leg.getMode());
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
						Leg leg = (Leg) elements.get(idx + 1);
						PersonDepartureEvent deparutreEvent = new PersonDepartureEvent(actEndTime, plan.getPerson()
								.getId(), act.getLinkId(), leg.getMode());
						eventQueue.add(deparutreEvent);
					}

					prevEndTime = actEndTime;
				}
			}

			for (Event event : eventQueue) {
				try{
				eventManager.processEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		
		private double calcCarTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network) {
			double tt = 0;
			if (route.getStartLinkId() != route.getEndLinkId()) {

				List<Id> ids = route.getLinkIds();
				for (int i = 0; i < ids.size(); i++) {
					tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime, null, null);
					tt++;// 1 sec for each node
				}
				tt += travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), startTime, null, null);
			}

			return tt;
		}
		
		private double calcTransitWalkTime(Person person, Activity startAct, Activity endAct) {
			 return transitTravelTime.getTravelTime(person, startAct.getCoord(), endAct.getCoord());
		}
		
		private double calcPTTravelTime(Leg leg, double depTime, Person person) {
//			TransitLine line = schedule.getTransitLines().get(route.getLineId());
//			TransitRoute troute = line.getRoutes().get(route.getRouteId());
//			NetworkRoute nroute = troute.getRoute();
//			
//			TransitRouteStop accessStop = troute.getStop(schedule.getFacilities().get(route.getAccessStopId()));
//			double time = preparedSchedule.getNextDepartureTime(troute, accessStop, depTime);
//			int stopIdx = troute.getStops().indexOf(accessStop);
//			
//			List<Id> linkIds = nroute.getLinkIds();
//			for(int i = 0; i < linkIds.size(); i++) {
//				eventQueue.add(new LinkEnterEvent(time, person.getId(), linkIds.get(i), null));
//				time += transitTravelTime.getLinkTravelTime(network.getLinks().get(linkIds.get(i)), depTime, person, null);
//				eventQueue.add(new LinkLeaveEvent(time, person.getId(), linkIds.get(i), null));
//			}
//			return route.getTravelTime();//TODO
			return railSimEngine.simulate(person, leg, depTime);
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		executor.shutdown();
	}
}
