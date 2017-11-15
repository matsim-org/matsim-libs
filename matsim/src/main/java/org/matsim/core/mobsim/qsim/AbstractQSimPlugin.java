package org.matsim.core.mobsim.qsim;

import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractQSimPlugin {

	private Config config;

	public AbstractQSimPlugin(Config config) {
		this.config = config;
	}

	public final Config getConfig() {
		return config;
	}
	@SuppressWarnings("static-method")
	public Collection<? extends Module> modules() {
		return Collections.emptyList();
	}
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.emptyList();
	}
	public Collection<Class<? extends MobsimListener>> listeners() {
		return Collections.emptyList();
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
