package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

public class NetsimEngineContext {
	private final EventsManager events;
	final  double effectiveCellSize;
	private final AgentCounter agentCounter;
	final AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder;
	final QSimConfigGroup qsimConfig;
	private final MobsimTimer mobsimTimer;
	final SnapshotLinkWidthCalculator linkWidthCalculator;

	public NetsimEngineContext(EventsManager events, double effectiveCellSize, AgentCounter agentCounter,
			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder, QSimConfigGroup qsimConfig, MobsimTimer mobsimTimer, 
			SnapshotLinkWidthCalculator linkWidthCalculator) {
		this.events = events;
		this.effectiveCellSize = effectiveCellSize;
		this.agentCounter = agentCounter;
		this.snapshotInfoBuilder = snapshotInfoBuilder;
		this.qsimConfig = qsimConfig;
		this.mobsimTimer = mobsimTimer ;
		this.linkWidthCalculator = linkWidthCalculator;
	}

	MobsimTimer getSimTimer() {
		return mobsimTimer;
	}

	EventsManager getEventsManager() {
		return events;
	}

	AgentCounter getAgentCounter() {
		return agentCounter;
	}

}