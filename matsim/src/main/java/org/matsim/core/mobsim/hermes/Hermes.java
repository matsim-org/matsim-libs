package org.matsim.core.mobsim.hermes;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.mobsim.framework.Mobsim;

public final class Hermes implements Mobsim {

	final private static Logger log = Logger.getLogger(Hermes.class);
    private Realm realm;
    private Agent[] agents;
	private ScenarioImporter si; // TODO - I don't really need this!
	private final Scenario scenario;
    private final ParallelEventsManager eventsManager;

	public Hermes(Scenario scenario, EventsManager eventsManager) {
        this.scenario = scenario;
        this.eventsManager = (ParallelEventsManager) eventsManager;
    }

	private void importScenario() throws Exception {
		si = ScenarioImporter.instance(scenario, eventsManager);
		si.generate();
		this.realm = si.realm;
		this.agents = si.hermes_agents;
	}

	private void processEvents() {
        eventsManager.processEvents(realm.getSortedEvents());

		for (Agent agent : agents) {
			if (agent != null && !agent.finished() && !agent.isVehicle()) {
				int matsim_id = si.matsim_id(agent.id(),  false);
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
			log.info(String.format("ETHZ importing hermes scenario took %d ms", System.currentTimeMillis() - time));

			eventsManager.initProcessing();

			time = System.currentTimeMillis();
			realm.run();
			log.info(String.format(
					"ETHZ hermes took %d ms", System.currentTimeMillis() - time));

			time = System.currentTimeMillis();
			processEvents();
			eventsManager.finishProcessing();
			log.info(String.format("ETHZ matsim event processing took %d ms", System.currentTimeMillis() - time));

			// Launch scenario imported in background
			si.reset();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
