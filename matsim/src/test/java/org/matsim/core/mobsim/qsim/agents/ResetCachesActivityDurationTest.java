/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;

/**
 * Does {@code resetCaches()} during an activity keep its maximum duration measured from the activity start?
 * <p>
 * The agent arrives at "work" at 08:01 and stays for a maximum duration of one hour. Halfway through, within-day code
 * calls {@code resetCaches()} without changing the activity (as done after any plan change).
 *
 * @author nkuehnel / MOIA
 */
public class ResetCachesActivityDurationTest {

	private static final Id<Person> PERSON = Id.createPersonId("person");
	private static final double ARRIVAL = 8 * 3600 + 60;
	private static final double DURATION = 3600;

	@Test
	void testWithoutResetTheActivityEndsAfterItsDuration() {
		assertThat(workEnd(ActivityDurationInterpretation.tryEndTimeThenDuration, false)).isEqualTo(ARRIVAL + DURATION);
	}

	@Test
	void testResetKeepsTheDurationFromTheActivityStart() {
		assertThat(workEnd(ActivityDurationInterpretation.tryEndTimeThenDuration, true)).isEqualTo(ARRIVAL + DURATION);
	}

	@Test
	void testResetKeepsTheDurationFromTheActivityStartWithMinOfDurationAndEndTime() {
		assertThat(workEnd(ActivityDurationInterpretation.minOfDurationAndEndTime, true)).isEqualTo(ARRIVAL + DURATION);
	}

	@Test
	void testMessageKeepsTheActivityStart() {
		// distributed simulation: an agent moved to another partition keeps measuring the duration from the start
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Link link = createNetwork(scenario.getNetwork());
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(PERSON);
		Plan plan = factory.createPlan();
		Activity home = factory.createActivityFromLinkId("home", link.getId());
		home.setMaximumDuration(DURATION);
		plan.addActivity(home);
		plan.addLeg(walk(factory, link.getId()));
		plan.addActivity(factory.createActivityFromLinkId("work", link.getId()));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

		MobsimTimer timer = new MobsimTimer();
		timer.setSimStartTime(100);
		timer.setTime(100);
		EventsManager events = EventsUtils.createEventsManager();
		TimeInterpretation timeInterpretation = TimeInterpretation.create(config);
		BasicPlanAgentImpl agent = new BasicPlanAgentImpl(plan, scenario, events, timer, timeInterpretation);
		assertThat(agent.getActivityEndTime()).isEqualTo(100 + DURATION);

		timer.setTime(1800);
		BasicPlanAgentImpl moved = new BasicPlanAgentImpl(agent.toMessage(), scenario, events, timer,
				timeInterpretation);
		moved.resetCaches();
		assertThat(moved.getActivityEndTime()).isEqualTo(100 + DURATION);
	}

	private static double workEnd(ActivityDurationInterpretation interpretation, boolean resetHalfway) {
		Config config = ConfigUtils.createConfig();
		config.plans().setActivityDurationInterpretation(interpretation);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(24 * 3600);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Link link = createNetwork(scenario.getNetwork());
		scenario.getPopulation()
				.addPerson(createPerson(scenario.getPopulation().getFactory(), link.getId(), interpretation));

		EventsManager events = EventsUtils.createEventsManager();
		List<Double> workEnds = new ArrayList<>();
		events.addHandler((ActivityEndEventHandler)event -> {
			if (event.getActType().equals("work")) {
				workEnds.add(event.getTime());
			}
		});

		QSim qsim = new QSimBuilder(config).useDefaults().build(scenario, events);
		if (resetHalfway) {
			qsim.addQueueSimulationListeners((MobsimBeforeSimStepListener)(MobsimBeforeSimStepEvent e) -> {
				if (e.getSimulationTime() == ARRIVAL + DURATION / 2) {
					MobsimAgent agent = qsim.getAgents().get(PERSON);
					WithinDayAgentUtils.resetCaches(agent);
					qsim.rescheduleActivityEnd(agent);
				}
			});
		}
		qsim.run();

		assertThat(workEnds).hasSize(1);
		return workEnds.getFirst();
	}

	private static Link createNetwork(Network network) {
		Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
		Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(100, 0));
		network.addNode(a);
		network.addNode(b);
		Link link = network.getFactory().createLink(Id.createLinkId("ab"), a, b);
		network.addLink(link);
		return link;
	}

	private static Person createPerson(PopulationFactory factory, Id<Link> linkId,
			ActivityDurationInterpretation interpretation) {
		Person person = factory.createPerson(PERSON);
		Plan plan = factory.createPlan();
		Activity home = factory.createActivityFromLinkId("home", linkId);
		home.setEndTime(8 * 3600);
		plan.addActivity(home);
		plan.addLeg(walk(factory, linkId));
		Activity work = factory.createActivityFromLinkId("work", linkId);
		work.setMaximumDuration(DURATION);
		if (interpretation == ActivityDurationInterpretation.minOfDurationAndEndTime) {
			work.setEndTime(18 * 3600); // later than the duration-based end, so it does not bind
		}
		plan.addActivity(work);
		plan.addLeg(walk(factory, linkId));
		plan.addActivity(factory.createActivityFromLinkId("home", linkId));
		person.addPlan(plan);
		return person;
	}

	private static Leg walk(PopulationFactory factory, Id<Link> linkId) {
		Leg leg = factory.createLeg(TransportMode.walk);
		var route = RouteUtils.createGenericRouteImpl(linkId, linkId);
		route.setTravelTime(60);
		route.setDistance(50);
		leg.setRoute(route);
		leg.setTravelTime(60);
		return leg;
	}
}
