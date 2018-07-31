package org.matsim.contrib.dynagent.run;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class DynActivityEnginePlugin extends AbstractQSimPlugin {
	public final static String DYN_ACTIVITY_ENGINE_NAME = "DynActivityEngine";
	
	public DynActivityEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void configure() {
				bind(DynActivityEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends ActivityHandler>> activityHandlers() {
		return Collections.singletonMap(DYN_ACTIVITY_ENGINE_NAME, DynActivityEngine.class);
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(DYN_ACTIVITY_ENGINE_NAME, DynActivityEngine.class);
	}
}
