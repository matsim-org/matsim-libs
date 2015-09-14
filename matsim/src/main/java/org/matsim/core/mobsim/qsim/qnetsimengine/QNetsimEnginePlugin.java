package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class QNetsimEnginePlugin extends AbstractQSimPlugin {

	public QNetsimEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(QNetsimEngine.class).asEagerSingleton();
				bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		Collection<Class<? extends DepartureHandler>> result = new ArrayList<>();
		result.add(VehicularDepartureHandler.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(QNetsimEngine.class);
		return result;
	}
}
