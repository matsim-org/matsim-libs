package org.matsim.contrib.signals.controler;

import java.util.Collection;
import java.util.Collections;

import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class SignalsQSimPlugin extends AbstractQSimPlugin {
	public SignalsQSimPlugin(Config config) {
		super(config);
	}
	
	@SuppressWarnings("static-method")
	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {
				bind(QSimSignalEngine.class);
			}
		});
	}
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.emptyList();
	}
	public Collection<Class<? extends MobsimListener>> listeners() {
		return Collections.singleton(QSimSignalEngine.class);
	}
	public Collection<Class<? extends AgentSource>> agentSources() {
		return Collections.emptyList();
	}
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.emptyList();
	}
	public Collection<Class<? extends ActivityHandler>> activityHandlers() {
		return Collections.emptyList();
	}
}
