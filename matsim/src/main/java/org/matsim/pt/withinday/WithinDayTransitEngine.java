/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SimpleDisruptionEngine.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.withinday;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.jaxb.ptdisruption.XMLDisruptions;
import org.matsim.jaxb.ptdisruption.XMLDisruptions.XMLDisruption;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WithinDayTransitEngine implements MobsimEngine {

	private static Logger log = Logger.getLogger(WithinDayTransitEngine.class);
	
	private final Config config;
	private final Scenario scenario;
	
	//private final double disruptionStart;
	private List<XMLDisruption> disruptions;
	
	private InternalInterface internalInterface;
	
	private TransitSchedule currentSchedule;
	private Deque<XMLDisruption> pending;
	
	
	@Inject
	public WithinDayTransitEngine(Config config, Scenario scenario) {
		this.scenario = scenario;
		this.config = config;
		
		WithinDayTransitConfigGroup cfg = ConfigUtils.addOrGetModule(config, WithinDayTransitConfigGroup.class);
		
		this.disruptions = new ArrayList<>();
		try {
			JAXBContext jaxb = JAXBContext.newInstance(XMLDisruptions.class);
			Unmarshaller unmarshall = jaxb.createUnmarshaller();
			URL url = ConfigGroup.getInputFileURL(config.getContext(), cfg.getDisruptionsFile());
			XMLDisruptions ds = (XMLDisruptions) unmarshall.unmarshal(url);
			this.disruptions.addAll(ds.getDisruption());
			this.disruptions.sort((d1, d2) -> (int)Math.signum(d1.getStartTime() - d2.getStartTime()));
		} catch (JAXBException e) {
			log.error("Error while parsing disruptions XML file" , e);
		}
		this.pending = new ArrayDeque<>(disruptions.size());
	}
	
	@Override
	public void doSimStep(double time) {
		// Check whether there are disruptions queued up that should be executed now
		while (!pending.isEmpty() && time >= pending.peek().getStartTime()) {
			XMLDisruption disruption = pending.pop();
			
			if (log.isInfoEnabled()) {
				log.info("Disruption was triggered. Running the replanner for " + describe(disruption));
			}
			
			currentSchedule = reduceSchedule(currentSchedule, disruption);
			TransitRouter router = getRouter(currentSchedule);
			
			doReplan(router, disruption);
		}
	}
	
	/**
	 * Describe a disruption for logging purposes
	 * @param disruption the disruption to describe
	 * @return a description of the disruption
	 */
	private String describe(XMLDisruption disruption) {
		StringBuilder sb = new StringBuilder();
		sb.append("disruption with start time ");
		sb.append(Time.writeTime(disruption.getStartTime()));
		if (disruption.getEndTime() != null) {
			sb.append(" and end time ");
			sb.append(Time.writeTime(disruption.getEndTime()));
		}
		sb.append(" affecting line ");
		sb.append(disruption.getLine());
		if (disruption.getRoute() != null) {
			sb.append(" route ");
			sb.append(disruption.getRoute());
		}
		return sb.toString();
	}
	
	/**
	 * Scans through the agents in the simulation and calls replanAgent on them
	 * @param router the router to use for affected agents
	 * @param disruption the disruption that triggers the replanning
	 */
	private void doReplan(TransitRouter router, XMLDisruption disruption) {
		QSim qsim = (QSim) internalInterface.getMobsim();
		qsim.getAgents().values().forEach(agent -> replanAgent(agent, router, disruption));		
	}
	
	/**
	 * Replan an agent
	 * @param agent the agent to replan
	 * @param router the update router to use during replanning
	 * @param disruption the disruption that triggered this replanning call
	 */
	private void replanAgent(MobsimAgent agent, TransitRouter router, XMLDisruption disruption) {
		if (agent instanceof HasModifiablePlan) {
			HasModifiablePlan hmp = (HasModifiablePlan) agent;
			Person person = scenario.getPopulation().getPersons().get(agent.getId());
			
			Plan plan = hmp.getModifiablePlan();
			List<PlanElement> planElements = plan.getPlanElements();
			PlanElement current = WithinDayAgentUtils.getCurrentPlanElement(agent);
			List<PlanElement> remainingElements = dropUntil(planElements, current);
			
			if (!checkAffected(disruption, remainingElements)) {
				// This agent's plan is not affected by this disruption. Do nothing.
				return;
			}
			
			if (current instanceof Leg) {
				// Go to the next one, which has to be an Activity by definition
				// This means the current leg is still completed
				remainingElements.remove(0);
				current = remainingElements.get(0);
			} else if (current.equals(remainingElements.get(remainingElements.size()-1))) {
				// This agent is already at the last activity. Do nothing.
				return;
			}

			log.info("New plan computed for person "+person.getId());
			
			// Replan the remaining part of the agent's plan
			List<PlanElement> newPlan = replanFromActivity(disruption, remainingElements, router, person);
			int index = planElements.indexOf(current);
			while(index < planElements.size() - 1) {
				planElements.remove(planElements.size()-1);
			}
			planElements.addAll(newPlan);
		}
	}
	
	/**
	 * Takes the remaining plan of an agent which should start at an activity and replans it
	 * @param disruption the disruption that triggered this replanning action
	 * @param elements the remaining plan of the agent
	 * @param router the updated router to use during replanning
	 * @param person the person whose plan is affected
	 * @return a replanned plan generated using the updated router
	 */
	private List<PlanElement> replanFromActivity(XMLDisruption disruption, List<PlanElement> elements, TransitRouter router, Person person) {
		List<PlanElement> newPlan = new ArrayList<>();
		if (elements.size() < 3) {
			newPlan.addAll(elements);
			return newPlan;
		}
		List<Triplet<Activity, Leg, Activity>> triplets = toTriplets(elements);
		// TODO: I am tracing an estimate of the time in the plan.
		//       However, I am unsure how precise this estimate is... - pcbouman may'18
		double time = triplets.get(0).first.getEndTime();
		for (Triplet<Activity, Leg, Activity> triplet : triplets) {
			newPlan.add(triplet.first);
			Route route = triplet.second.getRoute();
			if (route instanceof ExperimentalTransitRoute) {
				ExperimentalTransitRoute etr = (ExperimentalTransitRoute) route;
				if (checkTransitRouteAffected(disruption, etr)) {
					FakeFacility origin = new FakeFacility(triplet.first.getCoord());
					FakeFacility destination = new FakeFacility(triplet.third.getCoord());
					List<Leg> newRoute = router.calcRoute(origin, destination, time, person);
					List<PlanElement> newPlanEls = addPTInteraction(newRoute);
					newPlan.addAll(newPlanEls);
					// Add pt-interactions
					for (Leg leg : newRoute) {
						time += leg.getTravelTime();
					}
					time += triplet.third.getMaximumDuration();
				}
				else {
					newPlan.add(triplet.second);
					time += triplet.second.getTravelTime();
					time += triplet.third.getMaximumDuration();
				}
			} else {
				newPlan.add(triplet.second);
			}
		}
		// Do not forget to add the final activity
		newPlan.add(triplets.get(triplets.size()-1).third);
		if (log.isDebugEnabled()) {
			log.debug("Old plan: "+elements+", New plan: "+newPlan);
		}
		return newPlan;
	}
	
	/**
	 * Injects 'pt interaction' activities into a list of legs
	 * @param legs a list of legs
	 * @return the same list of legs interspersed with 'pt interaction' activities
	 */
	private List<PlanElement> addPTInteraction(List<Leg> legs) {
		List<PlanElement> result = new ArrayList<>();
		for (int i=0; i < legs.size(); i++) {
			Leg leg = legs.get(i);
			Route route = leg.getRoute();
			result.add(leg);
			if (i < legs.size()-1) {
				Activity act = PopulationUtils.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, route.getEndLinkId());
				result.add(act);
			}
		}
		
		return result;
	}
	
	/**
	 * Convert a list of plan elements which alternate between activity and leg by definition, to
	 * a list of triplets such that each triplet is of the form (activity,leg,activity) and the
	 * last activity and first activity of two consecutive triplets are equal.
	 * @param els A list of planelements
	 * @return a list of (activity,leg,activity) triplets
	 */
	private static List<Triplet<Activity,Leg,Activity>> toTriplets(List<PlanElement> els) {
		List<Triplet<Activity,Leg,Activity>> result = new ArrayList<>();
		for (int i=0; i < els.size(); i += 2) {
			PlanElement el = els.get(i);
			if (el instanceof Activity) {
				i--; // Decrement by one
				continue; // Increment by two
			}
			if (i < 1) {
				continue; // No prior activity...
			}
			Activity prev = (Activity) els.get(i-1);
			Leg current = (Leg) els.get(i);
			Activity next = (Activity) els.get(i+1);
			result.add(Triplet.of(prev, current, next));
		}
		return result;
	}
	
	/**
	 * Create a list that only contains the elements after a certain element
	 * @param lst the original list
	 * @param cmp the first element which should be included in the output
	 * @return the truncated list
	 */
	private static <E> List<E> dropUntil(List<E> lst, E cmp) {
		// Note: Java versions >= 9 contain something like this in the Streams API
		List<E> result = new ArrayList<>();
		for (E el : lst) {
			if (!result.isEmpty() || el.equals(cmp)) {
				result.add(el);
			}
		}
		return result;
	}
	
	/**
	 * Provides a new TransitRouter based on a new schedule
	 * @param newSchedule the updated schedule
	 * @return a TransitRouter for this schedule
	 */
	private TransitRouter getRouter(TransitSchedule newSchedule) {
		TransitRouterConfig cfg = new TransitRouterConfig(
				config.planCalcScore(),
				config.plansCalcRoute(),
				config.transitRouter(),
				config.vspExperimental());
		TransitRouterImplFactory fac = new TransitRouterImplFactory(newSchedule, cfg);
		return fac.get();
	}
	
	/**
	 * This filters routes, lines and departures from a TransitSchedule according to the
	 * definition of a disruption. This is basically a rather involved semi deep-cloning method.
	 * @param base the original TransitSchedule
	 * @param disruption the disruption that should be used to filter the schedule
	 * @return the reduced schedule from which disrupted services are removed
	 */
	private TransitSchedule reduceSchedule(TransitSchedule base, XMLDisruption disruption) {
		// TODO: maybe it is important to copy the other attributes as well? - pcbouman may'18		
		TransitScheduleFactory fac = new TransitScheduleFactoryImpl();
		TransitSchedule result = fac.createTransitSchedule();
		base.getFacilities().forEach((id,f) -> result.addStopFacility(f));
		base.getAttributes().getAsMap().forEach((k,v) -> result.getAttributes().putAttribute(k,v));
		for (TransitLine line : base.getTransitLines().values()) {
			boolean addLine = false;
			TransitLine newLine = fac.createTransitLine(line.getId());
			line.getAttributes().getAsMap().forEach((k,v) -> newLine.getAttributes().putAttribute(k,v));
			newLine.setName(line.getName());
			for (TransitRoute route : line.getRoutes().values()) {
				boolean addRoute = false;
				TransitRoute newRoute = fac.createTransitRoute(route.getId(), route.getRoute(), route.getStops(), route.getTransportMode());
				for (Departure dep : route.getDepartures().values()) {
					if (checkDeparture(disruption, line, route, dep)) {
						newRoute.addDeparture(dep);
						addRoute = true;
					}
					else {
						log.info("Removing departure at "+Time.writeTime(dep.getDepartureTime())+" of route '"+route.getId()+"' of line '"+line.getId()+"'");
					}
				}
				if (addRoute) {
					newLine.addRoute(newRoute);
					addLine = true;
				}
			}
			if (addLine) {
				result.addTransitLine(newLine);
			}
			else {
				log.info("No routes left in line '"+line.getName()+"'. Removing it from the TransitSchedule.");
			}
			
		}
		return result;
	}
	
	/**
	 * Checks whether a plan is affected by a give disruption
	 * @param disruption the disruption to check for
	 * @param pes the plan to check
	 * @return whether the plan is (possibly) affected by this disruption
	 */
	private boolean checkAffected(XMLDisruption disruption, List<PlanElement> pes) {
		for (PlanElement pe : pes) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				Route route = leg.getRoute();
				if (route instanceof ExperimentalTransitRoute) {
					ExperimentalTransitRoute etr = (ExperimentalTransitRoute) route;
					if (checkTransitRouteAffected(disruption, etr)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks whether a transit route is affected by the disruption
	 * @param disruption the disruption to check for
	 * @param route the transit route to check
	 * @return whether the transit route contains lines or routes that are affected by the disruption
	 */
	private boolean checkTransitRouteAffected(XMLDisruption disruption, ExperimentalTransitRoute route) {
		if (!route.getLineId().equals(Id.create(disruption.getLine(), TransitLine.class))) {
			return false;
		}
		if (disruption.getRoute() != null && !route.getRouteId().equals(Id.create(disruption.getRoute(), TransitRoute.class))) {
			return false;
		}
		return true;
	}
	
	/**
	 * Checks whether a departure is affected by a disruption
	 * @param disruption the disruption to check for
	 * @param line the line of this departure
	 * @param route the route of this departure
	 * @param dep the actual departure
	 * @return whether the departure is affected by the disruption
	 */
	private boolean checkDeparture(XMLDisruption disruption, TransitLine line, TransitRoute route, Departure dep) {
		if (!line.getId().equals(Id.create(disruption.getLine(),TransitLine.class))) {
			return true;
		}
		if (disruption.getRoute() != null && !route.getId().equals(Id.create(disruption.getRoute(),TransitLine.class))) {
			return true;
		}
		if (disruption.getEndTime() != null && dep.getDepartureTime() >= disruption.getEndTime()) {
			return true;
		}
		if (dep.getDepartureTime() < disruption.getStartTime()) {
			return true;
		}
		return false;
	}

	@Override
	public void onPrepareSim() {
		// Reset the queue of pending disruptions
		pending.clear();
		pending.addAll(disruptions);
		// Reset the current transit schedule
		currentSchedule = scenario.getTransitSchedule();
	}

	@Override
	public void afterSim() {
		// Do nothing
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	// Helper class for triplets
	private static class Triplet<A,B,C> {
		public final A first;
		public final B second;
		public final C third;
		
		public Triplet(A first, B second, C third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

		public static <A,B,C> Triplet<A,B,C> of(A first, B second, C third) {
			return new Triplet<>(first,second,third);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((first == null) ? 0 : first.hashCode());
			result = prime * result + ((second == null) ? 0 : second.hashCode());
			result = prime * result + ((third == null) ? 0 : third.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Triplet<?,?,?> other = (Triplet<?,?,?>) obj;
			if (first == null) {
				if (other.first != null)
					return false;
			} else if (!first.equals(other.first))
				return false;
			if (second == null) {
				if (other.second != null)
					return false;
			} else if (!second.equals(other.second))
				return false;
			if (third == null) {
				if (other.third != null)
					return false;
			} else if (!third.equals(other.third))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "(" + first + ", " + second + ", " + third + ")";
		}
		
		
	}
	
}
