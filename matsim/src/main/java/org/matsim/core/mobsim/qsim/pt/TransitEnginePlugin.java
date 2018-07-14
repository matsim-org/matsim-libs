package org.matsim.core.mobsim.qsim.pt;

import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class TransitEnginePlugin extends AbstractQSimPlugin {
	public final static String TRANSIT_ENGINE_NAME = "TransitEngine";

	public TransitEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TransitQSimEngine.class).asEagerSingleton();
//				bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonMap(TRANSIT_ENGINE_NAME, TransitQSimEngine.class);
	}

	@Override
	public Map<String, Class<? extends AgentSource>> agentSources() {
		return Collections.singletonMap(TRANSIT_ENGINE_NAME, TransitQSimEngine.class);
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(TRANSIT_ENGINE_NAME, TransitQSimEngine.class);
	}
}
