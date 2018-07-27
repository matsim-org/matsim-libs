package org.matsim.withinday.mobsim;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class WithinDayQSimPlugin extends AbstractQSimPlugin {
	public static String WITHIN_DAY_ENGINE = "WithinDayEngine";
	
	private final WithinDayEngine withinDayEngine;
	private final FixedOrderSimulationListener fixedOrderSimulationListener; 
	private final WithinDayTravelTime withinDayTravelTime;

	public WithinDayQSimPlugin(Config config, WithinDayEngine withinDayEngine, FixedOrderSimulationListener fixedOrderSimulationListener, WithinDayTravelTime withinDayTravelTime) {
		super(config);
		
		this.withinDayEngine = withinDayEngine;
		this.fixedOrderSimulationListener = fixedOrderSimulationListener;
		this.withinDayTravelTime = withinDayTravelTime;
	}

	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {
				bind(WithinDayEngine.class).toInstance(withinDayEngine);
				bind(FixedOrderSimulationListener.class).toInstance(fixedOrderSimulationListener);
				bind(WithinDayTravelTime.class).toInstance(withinDayTravelTime);
			}
		});
	}

	public Collection<Class<? extends MobsimListener>> listeners() {
		return Arrays.asList(FixedOrderSimulationListener.class, WithinDayTravelTime.class);
	}

	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(WITHIN_DAY_ENGINE, WithinDayEngine.class);
	}
}
