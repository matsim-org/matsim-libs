package org.matsim.core.mobsim.qsim;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class TeleportationPlugin extends AbstractQSimPlugin {

	@Override
	public Collection<? extends AbstractModule> modules() {
		Collection<AbstractModule> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void install() {
				bind(TeleportationEngine.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(TeleportationEngine.class);
		return result;
	}
}
