/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.vis.otfvis.interfaces.PlayPauseSimulationControlI;
import org.matsim.vis.snapshotwriters.VisMobsim;

import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

/**
 * Extracted the play/pause functionality from otfvis to make it available for other purposes (specifically, RMITs 
 * CombineSim approach).
 * 
 * @author nagel
 */
public class PlayPauseSimulationControl implements PlayPauseSimulationControlI {

	public enum Status {
		PAUSE, PLAY
	}

	private volatile Status status = Status.PLAY;

	private final Semaphore accessToQNetwork = new Semaphore(1, true);

	private volatile double localTime = -1;

	private final Phaser stepDone = new Phaser(1);

	public PlayPauseSimulationControl(VisMobsim qSim) {
		PlayPauseMobsimListener playPauseMobsimListener = new PlayPauseMobsimListener();
		qSim.addQueueSimulationListeners(playPauseMobsimListener);
	}

	private class PlayPauseMobsimListener implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener, MobsimBeforeCleanupListener {
		@Override
		public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
			try {
				accessToQNetwork.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
			accessToQNetwork.release();
			localTime = (int) event.getSimulationTime();
			stepDone.arriveAndAwaitAdvance();
		}
		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			localTime = Double.MAX_VALUE;
			stepDone.arriveAndDeregister();
		}
	}

	@Override
	public final void doStep(int time) {
		if (status == Status.PLAY) {
			throw new IllegalStateException();
		}
		while (localTime < time) {
			stepDone.arriveAndAwaitAdvance();
		}
	}

	@Override
	public final void pause() {
		if (status != Status.PAUSE) {
			stepDone.register();
			status = Status.PAUSE;
		}
	}

	@Override
	public final void play() {
		if (status != Status.PLAY) {
			stepDone.arriveAndDeregister();
			status = Status.PLAY;
		}
	}

	// for everything below here, I am not yet sure which of these need to be there. kai, mar'15

	Semaphore getAccessToQNetwork() {
		return accessToQNetwork;
	}

	// purely observational only below this line (probably not a problem)
	
	boolean isFinished() {
		return ( localTime == Double.MAX_VALUE ) ;
	}

	public double getLocalTime() {
		return localTime;
	}

}
