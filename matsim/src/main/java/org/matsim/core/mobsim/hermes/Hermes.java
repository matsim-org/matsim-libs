/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.hermes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;

final class Hermes implements Mobsim {

	final private static Logger log = LogManager.getLogger(Hermes.class);
	private Realm realm;
	private Agent[] agents;
	private ScenarioImporter scenarioImporter;
	private final Scenario scenario;
	private final EventsManager eventsManager;

	public Hermes(Scenario scenario, EventsManager eventsManager) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
	}

	private void importScenario() throws Exception {
		scenarioImporter = ScenarioImporter.instance(scenario, eventsManager);
		scenarioImporter.generate();
		this.realm = scenarioImporter.realm;
		this.agents = scenarioImporter.hermesAgents;
	}

	private void processEvents() {
        eventsManager.processEvents(realm.getSortedEvents());

		for (Agent agent : agents) {
			if (agent != null && !agent.finished() && !agent.isTransitVehicle()) {
				int matsim_id = scenarioImporter.matsim_id(agent.id(), false);
				eventsManager.processEvent(
						new PersonStuckEvent(
								HermesConfigGroup.SIM_STEPS, Id.get(matsim_id, Person.class), Id.createLinkId("0"), "zero"));
			}
		}
	}

	@Override
	public void run() {
		long time;
		try {
			time = System.currentTimeMillis();
			importScenario();
			log.info(String.format("Hermes importing scenario took %d ms", System.currentTimeMillis() - time));

			eventsManager.initProcessing();

			time = System.currentTimeMillis();
			realm.run();
			log.info(String.format(
					"Hermes took %d ms", System.currentTimeMillis() - time));

			time = System.currentTimeMillis();
			processEvents();
			eventsManager.finishProcessing();
			log.info(String.format("Hermes MATSim event processing took %d ms", System.currentTimeMillis() - time));

			// Launch scenario imported in background
			scenarioImporter.reset();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
