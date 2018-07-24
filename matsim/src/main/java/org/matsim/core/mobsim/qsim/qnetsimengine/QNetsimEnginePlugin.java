package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class QNetsimEnginePlugin extends AbstractQSimPlugin {
	public final static String NETSIM_ENGINE_NAME = "NetsimEngine";

	public QNetsimEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			protected void configure() {
				bind(QNetsimEngine.class).asEagerSingleton();
				bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonMap(NETSIM_ENGINE_NAME, VehicularDepartureHandler.class);
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(NETSIM_ENGINE_NAME, QNetsimEngine.class);
	}
}
