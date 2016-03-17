package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;

public class QueueWithBufferContext {
	final EventsManager events;
	final  double effectiveCellSize;
	final AgentCounter agentCounter;
	final AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder;
	final QSimConfigGroup qsimConfig;

	public QueueWithBufferContext(EventsManager events, double effectiveCellSize, AgentCounter agentCounter,
			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder, QSimConfigGroup qsimConfig) {
		this.events = events;
		this.effectiveCellSize = effectiveCellSize;
		this.agentCounter = agentCounter;
		this.snapshotInfoBuilder = snapshotInfoBuilder;
		this.qsimConfig = qsimConfig;
	}

}