package org.matsim.contrib.hybridsim.simulation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.contrib.hybridsim.utils.IdIntMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridNetworkFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class HybridQSimPlugin extends AbstractQSimPlugin {
	static public String HYBRID_EXTERNAL_ENGINE = "HybridExternalEngine";

	public HybridQSimPlugin(Config config) {
		super(config);
	}

	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Provides
			@Singleton
			ExternalEngine provideExternalEngine(EventsManager eventsManager, QSim qSim, IdIntMapper mapper,
					HybridNetworkFactory networkFactory) {
				ExternalEngine engine = new ExternalEngine(eventsManager, qSim, mapper);
				networkFactory.setExternalEngine(engine);
				return engine;
			}
		});
	}

	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(HYBRID_EXTERNAL_ENGINE, ExternalEngine.class);
	}
}
