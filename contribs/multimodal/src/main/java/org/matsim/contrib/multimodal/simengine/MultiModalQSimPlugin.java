package org.matsim.contrib.multimodal.simengine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MultiModalQSimPlugin extends AbstractQSimPlugin {
	static public String MULTIMODAL_ENGINE = "MultimodalEngine";
	static public String MULTIMODAL_DEPARTURE_HANDLER = "MultimodalDepartureHandler";

	private final Map<String, TravelTime> multiModalTravelTimes;

	public MultiModalQSimPlugin(Config config, Map<String, TravelTime> multiModalTravelTimes) {
		super(config);
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {

			}

			@Provides
			@Singleton
			MultiModalSimEngine provideMultiModalSimEngine(MultiModalConfigGroup multiModalConfigGroup) {
				return new MultiModalSimEngine(multiModalTravelTimes, multiModalConfigGroup);
			}

			@Provides
			@Singleton
			MultiModalDepartureHandler provideMultiModalDepartureHandler(MultiModalSimEngine multiModalEngine,
					MultiModalConfigGroup multiModalConfigGroup) {
				return new MultiModalDepartureHandler(multiModalEngine, multiModalConfigGroup);
			}
		});
	}

	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(MULTIMODAL_ENGINE, MultiModalSimEngine.class);
	}

	public Map<String, Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonMap(MULTIMODAL_DEPARTURE_HANDLER, MultiModalDepartureHandler.class);
	}
}
