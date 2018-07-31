package org.matsim.core.mobsim.qsim;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class ActivityEnginePlugin extends AbstractQSimPlugin {
	public static final String ACTIVITY_ENGINE_NAME = "ActivityEngine";

	public ActivityEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void configure() {
				bind(ActivityEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends ActivityHandler>> activityHandlers() {
		return Collections.singletonMap(ACTIVITY_ENGINE_NAME, ActivityEngine.class);
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(ACTIVITY_ENGINE_NAME, ActivityEngine.class);
	}
}
