
/* *********************************************************************** *
 * project: org.matsim.*
 * NetsimEngineContext.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

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