/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;

import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

public class OTFVisMobsimListener implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener, MobsimBeforeCleanupListener {

	private final OnTheFlyServer server;

	public OTFVisMobsimListener(OnTheFlyServer server) {
		this.server = server;
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent event) {
		this.server.getSnapshotReceiver().finish();
	}


	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		this.server.blockUpdates();
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
		double time = event.getSimulationTime();
		this.server.unblockUpdates();
		this.server.updateStatus(time);
	}

}