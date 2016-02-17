package org.matsim.contrib.taxi.run;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class DynActivityEnginePlugin extends AbstractQSimPlugin {

	public DynActivityEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void configure() {
				bind(DynActivityEngine.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends ActivityHandler>> activityHandlers() {
		Collection<Class<? extends ActivityHandler>> result = new ArrayList<>();
		result.add(DynActivityEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(DynActivityEngine.class);
		return result;
	}
}
