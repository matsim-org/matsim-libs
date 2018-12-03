package org.matsim.contrib.multimodal.simengine;

import java.util.Map;

import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MultiModalQSimModule extends AbstractQSimModule {
	static final public String COMPONENT_NAME = "Multimodal";

	private final Map<String, TravelTime> multiModalTravelTimes;

	public MultiModalQSimModule(Map<String, TravelTime> multiModalTravelTimes) {
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	protected void configureQSim() {
		this.addQSimComponentBinding( COMPONENT_NAME ).to( MultiModalSimEngine.class ) ;
		this.addQSimComponentBinding( COMPONENT_NAME ).to( MultiModalDepartureHandler.class ) ;
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
	
	static public void configureComponents(QSimComponentsConfig components) {
		components.addNamedComponent(COMPONENT_NAME);
	}
}
