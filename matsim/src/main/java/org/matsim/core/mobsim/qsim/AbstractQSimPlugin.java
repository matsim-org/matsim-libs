package org.matsim.core.mobsim.qsim;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractQSimPlugin {

	public Collection<? extends AbstractModule> modules() {
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
