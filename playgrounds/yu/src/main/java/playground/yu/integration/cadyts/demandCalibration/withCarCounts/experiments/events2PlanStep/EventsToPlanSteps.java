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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.events2PlanStep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import cadyts.demand.Plan;
import cadyts.demand.PlanBuilder;
import cadyts.demand.PlanStep;

public class EventsToPlanSteps implements PersonDepartureEventHandler,
		LinkEnterEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private final Map<PlanImpl, PlanBuilder<Link>> planStepFactories = new HashMap<PlanImpl, PlanBuilder<Link>>();
	private final Map<PlanImpl, Plan<Link>> planStepsMap = new HashMap<PlanImpl, Plan<Link>>();
	private final Set<PlanImpl> finishedPlans = new HashSet<PlanImpl>();
	private Network net;
	private Population pop;

	private static final int MAX_TIME = 24 * 3600 - 1;

	private boolean foundPT = false;

	public EventsToPlanSteps(final Network net, final Population pop) {
		this.net = net;
		this.pop = pop;
	}

	private PlanBuilder<Link> getPlanStepFactoryOfPlan(PlanImpl plan,
			boolean throwException) {
		PlanBuilder<Link> planStepFactory = planStepFactories.get(plan);
		if (planStepFactory == null) {
			if (throwException) {
				throw new RuntimeException(
						"EventsToPlanSteps:\tEntering or arrival without Entry??");
			}

			planStepFactory = new PlanBuilder<Link>();
			planStepFactories.put(plan, planStepFactory);
		}
		return planStepFactory;
	}

	private Link getEventLink(Id<Link> linkId) {
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

	private PlanImpl getPlanFromEvent(Id<Person> personId) {
		Person person = pop.getPersons().get(personId);
		if (person == null) {
			throw new RuntimeException(
					"EventsToPlanSteps:\tPerson of an event [] does NOT exist in Population!!!");
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
	public void handleEvent(PersonDepartureEvent event) {
		PlanImpl plan = getPlanFromEvent(event.getPersonId());
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
		PlanImpl plan = getPlanFromEvent(event.getPersonId());
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan,
				true);
		int time = (int) event.getTime();
		if (time > MAX_TIME) {
			planStepFactory.addExit(MAX_TIME);
			endPlanStepFactory(planStepFactory, plan);
			return;
		}
		planStepFactory.addTurn(getEventLink(event.getLinkId()), time);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// removePlanFactory, if the arrivalEvent ist the last one?? --> clearUp
		// will be done afterMobsim
		PlanImpl plan = getPlanFromEvent(event.getPersonId());
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan,
				true);
		int time = (int) event.getTime();
		if (time > MAX_TIME) {
			planStepFactory.addExit(MAX_TIME);
			endPlanStepFactory(planStepFactory, plan);
			return;
		}
		planStepFactory.addExit(time);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		PlanImpl plan = getPlanFromEvent(event.getPersonId());
		if (finishedPlans.contains(plan)) {
			return;
		}
		PlanBuilder<Link> planStepFactory = getPlanStepFactoryOfPlan(plan,
				true);
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
				.println("MATSim_Integration.DemandCalibration:\tsize of planStepFactories:\t"
						+ planStepFactories.size());
		for (PlanImpl plan : planStepFactories.keySet()) {
			planStepsMap
					.put(plan, planStepFactories.get(plan).getResult());
		}
		planStepFactories.clear();
		System.out
				.println("MATSim_Integration.DemandCalibration:\tsize of planStepsMap:\t"
						+ planStepsMap.size());
		// finishedPlans.add(plan) is not more necessary in this iteration
	}

	public static void main(String[] args) {
		NetworkImpl net = NetworkImpl.createNetwork();
		Random r = MatsimRandom.getLocalInstance();
		for (int i = 0; i < 10; i++) {
			net.createAndAddLink(Id.create(i, Link.class), net.createAndAddNode(
					Id.create(i + "f", Node.class), new CoordImpl(r.nextDouble() * 50000d,
							r.nextDouble() * 50000d)), net.createAndAddNode(
					Id.create(i + "t", Node.class), new CoordImpl(r.nextDouble() * 50000d,
							r.nextDouble() * 50000d)), r.nextInt(1000), r
					.nextInt(8) * 20, r.nextInt(10) * 200, 1);
		}
        Population pop = PopulationUtils.createPopulation(((ScenarioImpl) null).getConfig(), ((ScenarioImpl) null).getNetwork());
		for (int i = 0; i < 10; i++) {
			Person person = new PersonImpl(Id.create(i, Person.class));
			pop.addPerson(person);
			person.addPlan(new PlanImpl());
		}
		EventsManager events = EventsUtils
				.createEventsManager();
		EventsToPlanSteps e2p = new EventsToPlanSteps(net, pop);
		events.addHandler(e2p);
		loop: for (int i = 0; i < 10; i++) {
			Id<Person> agentId = Id.create(i, Person.class);
			double time = r.nextDouble() * 3600d * 30d;
			Id<Link> actLinkId = Id.create(r.nextInt(10), Link.class);
			for (int c = 0; c < r.nextInt(5) + 1; c++) {
				System.out.println("Person:\t" + agentId);
				// departure
				{
					Event event = new PersonDepartureEvent(time,
							agentId, actLinkId, "car");
					System.out.println("Event\tDeparture\t" + event);
					events.processEvent(event);
				}
				time += r.nextDouble() * 3d;
				// enterS
				Id<Link> linkId = null;
				for (int j = 0; j < r.nextInt(10); j++) {
					linkId = Id.create(r.nextInt(10), Link.class);
					LinkImpl link = (LinkImpl) net.getLinks().get(linkId);
					{
						Event event = new LinkEnterEvent(time,
								agentId, linkId, null);
						System.out.println("Event\tEntering\t" + event);
						events.processEvent(event);
					}
					time += link.getFreespeedTravelTime()
							* (r.nextDouble() * 3d + 1d);
					if (r.nextDouble() < 0.05) {
						Event event = new PersonStuckEvent(time,
								agentId, linkId, "car");
						System.out.println("Event\tStuck\t" + event);
						events.processEvent(event);
						continue loop;
					}
				}
				// arrival
				{
					linkId = linkId != null ? linkId : actLinkId;
					Event event = new PersonArrivalEvent(time,
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
