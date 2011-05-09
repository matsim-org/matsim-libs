/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToPlanSteps.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormalWithEvents2PlanSteps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import cadyts.demand.Plan;
import cadyts.demand.PlanBuilder;
import cadyts.demand.PlanStep;

public class Events2PlanSteps implements AgentDepartureEventHandler,
		LinkEnterEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {

	private final Map<PlanImpl, PlanBuilder<Link>> planStepFactories = new HashMap<PlanImpl, PlanBuilder<Link>>();
	private final Map<PlanImpl, Plan<Link>> planStepsMap = new HashMap<PlanImpl, Plan<Link>>();
	private final Set<PlanImpl> finishedPlans = new HashSet<PlanImpl>();
	private final Network net;
	private final Population pop;

	private static final int MAX_TIME = 24 * 3600 - 1;

	public Events2PlanSteps(final Network net, final Population pop) {
		this.net = net;
		this.pop = pop;
	}

	private PlanBuilder<Link> getPlanStepFactoryOfPlan(PlanImpl plan,
			boolean throwException) {
		PlanBuilder<Link> planStepFactory = planStepFactories.get(plan);
		if (planStepFactory == null) {
			if (throwException) {
				throw new RuntimeException(
						"Entering or arrival without Entry??");
			}

			planStepFactory = new PlanBuilder<Link>();
			planStepFactories.put(plan, planStepFactory);
		}
		return planStepFactory;
	}

	private Link getEventLink(Id linkId) {
		Link link = net.getLinks().get(linkId);
		if (link == null) {
			throw new RuntimeException(
					"EventsToPlanSteps:\tan event happens on a link that does NOT exist in network!!!");
		}
		return link;
	}

	private int getExitTime(int time) {
		return Math.min(MAX_TIME, time);
	}

	private PlanImpl getPlanFromEvent(PersonEvent event) {
		Person person = pop.getPersons().get(event.getPersonId());
		if (person == null) {
			throw new RuntimeException(
					"EventsToPlanSteps:\tPerson of an event [" + event
							+ "] does NOT exist in Population!!!");
		}
		PlanImpl plan = (PlanImpl) person.getSelectedPlan();
		return plan;
	}

	public Plan<Link> getPlanSteps(PlanImpl plan) {
		return planStepsMap.get(plan);
	}

	/*
	 * public final void convert(final PlanImpl plan) {
	 * 
	 * if (PlanModeJudger.usePt(plan)) { if (!foundPT) {
	 * System.out.println("BSE: found a PT plan"); foundPT = true; }
	 * planStepFactory.reset(); return; }
	 * 
	 * double time = plan.getFirstActivity().getEndTime(); if (time > MAX_TIME)
	 * return; for (int i = 1; i < plan.getPlanElements().size() - 1; i += 2) {
	 * // that's the leg we're gonna handle now LegImpl leg = (LegImpl)
	 * plan.getPlanElements().get(i); ActivityImpl fromAct = (ActivityImpl)
	 * plan.getPreviousActivity(leg); ActivityImpl toAct = (ActivityImpl)
	 * plan.getNextActivity(leg);
	 * 
	 * // entry
	 * planStepFactory.addEntry(network.getLinks().get(fromAct.getLinkId()),
	 * (int) time); // turns for all links for (Id linkId : ((NetworkRoute)
	 * leg.getRoute()).getLinkIds()) { Link link =
	 * network.getLinks().get(linkId); planStepFactory.addTurn(link, (int)
	 * time); time += ttime.getLinkTravelTime(link, time); if (time > MAX_TIME)
	 * { planStepFactory.addExit(MAX_TIME); return; } } // last turn to
	 * destination link
	 * planStepFactory.addTurn(network.getLinks().get(toAct.getLinkId()), (int)
	 * time); time += ttime.getLinkTravelTime(network.getLinks().get(
	 * toAct.getLinkId()), time); if (time > MAX_TIME) {
	 * planStepFactory.addExit(MAX_TIME); return; } // regular exit at end of
	 * leg planStepFactory.addExit((int) time); // advance time if
	 * (toAct.getDuration() != Time.UNDEFINED_TIME && toAct.getEndTime() !=
	 * Time.UNDEFINED_TIME) time = Math.max(time, Math.min(time +
	 * toAct.getDuration(), toAct.getEndTime())); else if (toAct.getDuration()
	 * != Time.UNDEFINED_TIME) time += toAct.getDuration(); else time =
	 * Math.max(time, toAct.getEndTime()); } // for-loop: handle next leg }
	 */

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		PlanImpl plan = getPlanFromEvent(event);
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan,
				false);
		int time = (int) event.getTime();
		if (time > MAX_TIME) {// DON'T add entry in time>MAX_TIME, it has had an
			// exit
			endPlanStepFactory(planStepFactory, plan);
			return;
		}
		planStepFactory.addEntry(getEventLink(event.getLinkId()), time);
	}

	@Override
	public void reset(int iteration) {
		finishedPlans.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		PlanImpl plan = getPlanFromEvent(event);
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan, true);
		int time = (int) event.getTime();
		if (time > MAX_TIME) {
			planStepFactory.addExit(MAX_TIME);
			endPlanStepFactory(planStepFactory, plan);
			return;
		}
		planStepFactory.addTurn(getEventLink(event.getLinkId()), time);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		// removePlanFactory, if the arrivalEvent ist the last one?? --> clearUp
		// will be done afterMobsim
		PlanImpl plan = getPlanFromEvent(event);
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan, true);
		int time = (int) event.getTime();
		if (time > MAX_TIME) {
			planStepFactory.addExit(MAX_TIME);
			endPlanStepFactory(planStepFactory, plan);
			return;
		}
		planStepFactory.addExit(time);
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		PlanImpl plan = getPlanFromEvent(event);
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan, true);
		planStepFactory.addExit(getExitTime((int) event.getTime()));
		endPlanStepFactory(planStepFactory, plan);
		// if planStepFactory==null, this planSteps could have been already
		// completed because of oversteping of MAX_TiME
	}

	/**
	 * @param planStepFactory
	 * @param plan
	 */
	private void endPlanStepFactory(PlanBuilder<Link> planStepFactory,
			PlanImpl plan) {
		planStepsMap.put(plan, planStepFactory.getResult());
		removePlanStepFactory(plan);//
		finishedPlans.add(plan);
	}

	private void removePlanStepFactory(PlanImpl plan) {
		planStepFactories.remove(plan);
	}

	/**
	 * this should be done by removeWorstPlan in StrategyManager
	 * 
	 * @param plan
	 */
	public void removePlanSteps(PlanImpl plan) {
		planStepsMap.remove(plan);
	}

	public void clearUpAfterMobsim() {
		System.out
				.println("MATSim_Integration parameterCalibration:\tsize of planStepFactories:\t"
						+ planStepFactories.size());
		for (PlanImpl plan : planStepFactories.keySet()) {
			planStepsMap.put(plan, planStepFactories.get(plan).getResult());
		}
		planStepFactories.clear();
		System.out
				.println("MATSim_Integration parameterCalibration:\tsize of planStepsMap:\t"
						+ planStepsMap.size());
		// finishedPlans.add(plan) is not more necessary in this iteration
	}

	public static void main(String[] args) {
		NetworkImpl net = NetworkImpl.createNetwork();
		Random r = MatsimRandom.getLocalInstance();
		for (int i = 0; i < 10; i++) {
			net.createAndAddLink(new IdImpl(i), net.createAndAddNode(
					new IdImpl(i + "f"), new CoordImpl(r.nextDouble() * 50000d,
							r.nextDouble() * 50000d)), net.createAndAddNode(
					new IdImpl(i + "t"), new CoordImpl(r.nextDouble() * 50000d,
							r.nextDouble() * 50000d)), r.nextInt(1000), r
					.nextInt(8) * 20, r.nextInt(10) * 200, 1);
		}
		Population pop = new PopulationImpl(null);
		for (int i = 0; i < 10; i++) {
			Person person = new PersonImpl(new IdImpl(i));
			pop.addPerson(person);
			person.addPlan(new PlanImpl());
		}
		EventsManager events = EventsUtils.createEventsManager();
		Events2PlanSteps e2p = new Events2PlanSteps(net, pop);
		events.addHandler(e2p);
		loop: for (int i = 0; i < 10; i++) {
			Id agentId = new IdImpl(i);
			double time = r.nextDouble() * 3600d * 30d;
			Id actLinkId = new IdImpl(r.nextInt(10));
			for (int c = 0; c < r.nextInt(5) + 1; c++) {
				System.out.println("Person:\t" + agentId);
				// departure
				{
					PersonEvent event = new AgentDepartureEventImpl(time,
							agentId, actLinkId, "car");
					System.out.println("Event\tDeparture\t" + event);
					events.processEvent(event);
				}
				time += r.nextDouble() * 3d;
				// enterS
				Id linkId = null;
				for (int j = 0; j < r.nextInt(10); j++) {
					linkId = new IdImpl(r.nextInt(10));
					LinkImpl link = (LinkImpl) net.getLinks().get(linkId);
					{
						PersonEvent event = new LinkEnterEventImpl(time,
								agentId, linkId);
						System.out.println("Event\tEntering\t" + event);
						events.processEvent(event);
					}
					time += link.getFreespeedTravelTime()
							* (r.nextDouble() * 3d + 1d);
					if (r.nextDouble() < 0.05) {
						PersonEvent event = new AgentStuckEventImpl(time,
								agentId, linkId, "car");
						System.out.println("Event\tStuck\t" + event);
						events.processEvent(event);
						continue loop;
					}
				}
				// arrival
				{
					linkId = linkId != null ? linkId : actLinkId;
					PersonEvent event = new AgentArrivalEventImpl(time,
							agentId, linkId, "car");
					System.out.println("Event\tArrival\t" + event);
					events.processEvent(event);
					actLinkId = linkId;
				}
				time += r.nextDouble() * 8d;
			}
		}
		// afterMobsim
		e2p.clearUpAfterMobsim();
		for (Person person : pop.getPersons().values()) {
			System.out.println("person ID:\t" + person.getId());

			Plan<Link> planSteps = e2p.getPlanSteps((PlanImpl) person
					.getSelectedPlan());
			for (int i = 0; i < planSteps.size(); i++) {
				PlanStep<Link> planStep = planSteps.getStep(i);
				System.out.println("\tplanStep:\tentryTime:\t"
						+ planStep.getEntryTime_s() + "\tlink ID:\t"
						+ planStep.getLink().getId());
			}
		}
	}
}
