package org.matsim.core.mobsim.hermes;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class HermesProvider implements Provider<Mobsim> {

	private final Scenario scenario;
    private final EventsManager eventsManager;

    @Inject
	public HermesProvider(Scenario scenario, EventsManager eventsManager) {
        this.scenario = scenario;
        this.eventsManager = eventsManager;
    }

	@Override
	public Mobsim get() {
		return new Hermes(scenario, eventsManager);
	}

}
