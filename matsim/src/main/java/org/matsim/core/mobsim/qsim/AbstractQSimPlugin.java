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
import java.util.Map;

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
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.emptyMap();
	}
	public Collection<Class<? extends MobsimListener>> listeners() {
		return Collections.emptyList();
	}
	public Map<String, Class<? extends AgentSource>> agentSources() {
		return Collections.emptyMap();
	}
	public Map<String, Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.emptyMap();
	}
	public Map<String, Class<? extends ActivityHandler>> activityHandlers() {
		return Collections.emptyMap();
	}

}
