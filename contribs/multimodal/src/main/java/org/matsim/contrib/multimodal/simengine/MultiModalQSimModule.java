package org.matsim.contrib.multimodal.simengine;

import java.util.Map;

import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MultiModalQSimModule extends AbstractQSimModule {
	static public String MULTIMODAL_ENGINE = "MultimodalEngine";
	static public String MULTIMODAL_DEPARTURE_HANDLER = "MultimodalDepartureHandler";

	private final Map<String, TravelTime> multiModalTravelTimes;

	public MultiModalQSimModule(Map<String, TravelTime> multiModalTravelTimes) {
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	protected void configureQSim() {
		bindMobsimEngine(MULTIMODAL_ENGINE).to(MultiModalSimEngine.class);
		bindDepartureHandler(MULTIMODAL_DEPARTURE_HANDLER).to(MultiModalDepartureHandler.class);
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
	
	static public void configureComponents(QSimComponents components) {
		components.activeMobsimEngines.add(MULTIMODAL_ENGINE);
		components.activeDepartureHandlers.add(MULTIMODAL_DEPARTURE_HANDLER);
	}
}
