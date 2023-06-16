
/* *********************************************************************** *
 * project: org.matsim.*
 * MessageQueueEngine.java
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

 package org.matsim.core.mobsim.qsim.messagequeueengine;

import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

import jakarta.inject.Inject;

class MessageQueueEngine implements MobsimBeforeSimStepListener {

	private final SteppableScheduler scheduler;

	@Inject
	MessageQueueEngine(final SteppableScheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		scheduler.doSimStep(e.getSimulationTime());
	}

}
