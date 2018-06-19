package org.matsim.core.mobsim.qsim.changeeventsengine;

import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.network.NetworkChangeEvent;

public interface NetworkChangeEventsEngineI extends MobsimEngine {
	void addNetworkChangeEvent(NetworkChangeEvent event);
}
