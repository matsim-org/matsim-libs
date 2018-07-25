package org.matsim.core.mobsim.qsim.changeeventsengine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class NetworkChangeEventsPlugin extends AbstractQSimPlugin {
	public final static String NETWORK_CHANGE_EVENTS_ENGINE = "NetworkChangeEventsEngine";

	public NetworkChangeEventsPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			protected void configure() {
				bind(NewNetworkChangeEventsEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(NETWORK_CHANGE_EVENTS_ENGINE, NewNetworkChangeEventsEngine.class);
	}
}
