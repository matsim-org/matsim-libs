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
package org.matsim.core.mobsim.framework;

import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

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

	private final Semaphore access = new Semaphore(1, true);

	private volatile double localTime = -1;

	private final Phaser stepDone = new Phaser(1);

	public PlayPauseSimulationControl(ObservableMobsim qSim) {
		PlayPauseMobsimListener playPauseMobsimListener = new PlayPauseMobsimListener();
		qSim.addQueueSimulationListeners(playPauseMobsimListener);
	}

	private class PlayPauseMobsimListener implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener, MobsimBeforeCleanupListener {
		@Override
		public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
			try {
				access.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
			access.release();
			localTime = (int) event.getSimulationTime();
			// yy I am not so sure about the "int".  kai, nov'17
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

	public final Semaphore getAccess() {
		return access;
	}

	public final boolean isFinished() {
		return ( localTime == Double.MAX_VALUE ) ;
	}

	public final double getLocalTime() {
		return localTime;
	}

}
