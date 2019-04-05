/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.util.Timer;

import javax.inject.Inject;

import java.util.HashMap;

/**
 * The starting point of the whole micro-simulation.
 * @see <a href="http://www.matsim.org/docs/jdeqsim">http://www.matsim.org/docs/jdeqsim</a>
 * @author rashid_waraich
 */
public class JDEQSimulation implements Mobsim {

	private final static Logger log = Logger.getLogger(JDEQSimulation.class);

	protected Scenario scenario;
	protected final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;

	private final JDEQSimConfigGroup config;
	private final EventsManager events;

	@Inject
	public JDEQSimulation(final JDEQSimConfigGroup config, final Scenario scenario, final EventsManager events) {
		Road.setConfig(config);
		Message.setEventsManager(events);
		this.config = config;
		this.scenario = scenario;
		this.events = events;
		activityDurationInterpretation = scenario.getConfig().plans().getActivityDurationInterpretation();
	}

	@Override
	public void run() {
		events.initProcessing();
		Timer timer = new Timer();
		timer.startTimer();

		Scheduler scheduler = new Scheduler(new MessageQueue(), config.getSimulationEndTime());
		initializeRoads(scheduler);
		initializeVehicles(scheduler);

		scheduler.startSimulation();

		timer.endTimer();
		log.info("Time needed for one iteration (only JDEQSimulation part): " + timer.getMeasuredTime() + "[ms]");
		events.finishProcessing();
	}

	protected void initializeRoads(Scheduler scheduler) {
		HashMap<Id<Link>, Road> allRoads = new HashMap<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			allRoads.put(link.getId(), new Road(scheduler, link));
		}
		Road.setAllRoads(allRoads);
	}

	protected void initializeVehicles(Scheduler scheduler) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			new Vehicle(scheduler, person, activityDurationInterpretation); // the vehicle registers itself to the scheduler
		}
	}

	public EventsManager getEvents() {
		return events;
	}

	public JDEQSimConfigGroup getConfig() {
		return config;
	}

}
