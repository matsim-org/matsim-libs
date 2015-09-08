package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class TransitEnginePlugin extends AbstractQSimPlugin {

	@Override
	public Collection<? extends AbstractModule> modules() {
		Collection<AbstractModule> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitQSimEngine.class).asEagerSingleton();
				bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		Collection<Class<? extends DepartureHandler>> result = new ArrayList<>();
		result.add(TransitQSimEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		Collection<Class<? extends AgentSource>> result = new ArrayList<>();
		result.add(TransitQSimEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(TransitQSimEngine.class);
		return result;
	}

}
