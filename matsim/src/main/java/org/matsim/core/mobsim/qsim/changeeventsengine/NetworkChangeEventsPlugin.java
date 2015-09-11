package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class NetworkChangeEventsPlugin extends AbstractQSimPlugin {

	@Override
	public Collection<? extends org.matsim.core.controler.AbstractModule> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void install() {
				bind(NewNetworkChangeEventsEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(NewNetworkChangeEventsEngine.class);
		return result;
	}
}
