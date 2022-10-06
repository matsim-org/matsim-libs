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

import java.util.HashMap;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.utils.timing.TimeInterpretation;


/**
 * The starting point of the whole micro-simulation.
 * @see <a href="http://www.matsim.org/docs/jdeqsim">http://www.matsim.org/docs/jdeqsim</a>
 * @author rashid_waraich
 */
public class JDEQSimulation implements Mobsim {

	private final static Logger log = LogManager.getLogger(JDEQSimulation.class);

	private final JDEQSimConfigGroup config;
	protected Scenario scenario;
	private final EventsManager events;
	private final TimeInterpretation timeInterpretation;

	@Inject
	public JDEQSimulation(final JDEQSimConfigGroup config, final Scenario scenario, final EventsManager events, final TimeInterpretation timeInterpretation) {
		Road.setConfig(config);
		Message.setEventsManager(events);
		this.config = config;
		this.scenario = scenario;
		this.events = events;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public void run() {
		events.initProcessing();
		Timer t = new Timer();
		t.startTimer();

		Scheduler scheduler = new Scheduler(new MessageQueue(), config.getSimulationEndTime().orElse(Double.MAX_VALUE));
		Road.setAllRoads(new HashMap<Id<Link>, Road>());

		// initialize network
		Road road;
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			road = new Road(scheduler, link);
			Road.getAllRoads().put(link.getId(), road);
		}

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			new Vehicle(scheduler, person, timeInterpretation); // the vehicle registers itself to the scheduler
		}

		scheduler.startSimulation();

		t.endTimer();
		log.info("Time needed for one iteration (only JDEQSimulation part): " + t.getMeasuredTime() + "[ms]");
		events.finishProcessing();
	}
}
