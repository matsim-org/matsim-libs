package org.matsim.core.mobsim.qsim;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class ActivityEnginePlugin extends AbstractQSimPlugin {

	@Override
	public Collection<? extends AbstractModule> modules() {
		Collection<AbstractModule> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void install() {
				bind(ActivityEngine.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends ActivityHandler>> activityHandlers() {
		Collection<Class<? extends ActivityHandler>> result = new ArrayList<>();
		result.add(ActivityEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(ActivityEngine.class);
		return result;
	}
}
