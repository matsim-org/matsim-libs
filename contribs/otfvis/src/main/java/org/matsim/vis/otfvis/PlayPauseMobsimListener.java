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
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.vis.otfvis.PlayPauseSimulationControl.AccessToBlockingEtc;

public class PlayPauseMobsimListener implements MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	private AccessToBlockingEtc myBarrier;

	public PlayPauseMobsimListener() {
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		if ( this.myBarrier==null ) {
			throw new RuntimeException("internalInterface==null.  Syntax is not yet stable; find out where the corresponding "
					+ "setter is.") ;
		}
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		this.myBarrier.blockOtherUpdates();
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
		double time = event.getSimulationTime();
		this.myBarrier.unblockOtherUpdates();
		this.myBarrier.updateStatus(time);
	}

	public void setBarrier(AccessToBlockingEtc myBarrier) {
		this.myBarrier = myBarrier ;
	}


}