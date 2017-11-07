package org.matsim.core.mobsim.qsim;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;

public final class ActivityEnginePlugin extends AbstractQSimPlugin {

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
	public Collection<Class<? extends ActivityHandler>> activityHandlers() {
		return Collections.singletonList(ActivityEngine.class);
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.singletonList(ActivityEngine.class);
	}
}
