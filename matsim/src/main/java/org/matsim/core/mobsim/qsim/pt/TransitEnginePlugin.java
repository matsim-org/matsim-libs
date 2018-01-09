package org.matsim.core.mobsim.qsim.pt;

import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;

public class TransitEnginePlugin extends AbstractQSimPlugin {

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
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonList(TransitQSimEngine.class);
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		return Collections.singletonList(TransitQSimEngine.class);
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.singletonList(TransitQSimEngine.class);
	}
}
