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

import java.util.concurrent.Semaphore;

/**
 * Extracted the play/pause functionality from otfvis to make it available for other purposes (specifically, RMITs 
 * CombineSim approach).
 * 
 * @author nagel
 */
public class PlayPauseSimulationControl implements PlayPauseSimulationControlI {

	public enum Status {
		PAUSE, PLAY, STEP, FINISHED
	}

	private volatile Status status = Status.PAUSE;
	// "volatile" seems to mean "can be changed by more than one thread", and it hedges against that by
	// ensuring that the object is changed as a whole before the next thread interferes.  The tutorial says
	// that still bad things can happen, but does not say which.  Seems to me that they mean that
	// there can still be race conditions, which is rather obvious. 
	// Problems here can occur when multiple threads (here mostly: the playpausecontrol and the 
	// mobsim itself) both try to modify state here. kai, jan'16
	
	private final Semaphore accessToQNetwork = new Semaphore(1, true);

	private volatile int localTime = 0;
	private volatile double stepToTime = 0;

	private final Object paused = new Object();
	private final Object stepDone = new Object();
	private final Object updateFinished = new Object();

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
			double time = event.getSimulationTime();
			accessToQNetwork.release();
			localTime = (int) time;
			if ( status == Status.STEP) {
				// Time and Iteration reached?
				if ( stepToTime <= localTime ) {
					synchronized (stepDone) {
						stepDone.notifyAll(); // releases all stepDone.wait()
						status = Status.PAUSE ;
					}
				}
			}
			synchronized(paused) {
				while (status == Status.PAUSE) {
					try {
						paused.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			status = Status.FINISHED;
		}
	}

	@Override
	public final void doStep(int time) {
		// yy seems to me that this is packing two functionalities into one:
		// (1) if time==0, step exactly one step.  For this, don't have to know the current time.
		// (2) if time is something else, step until this time.
		// ????
		
		// leave Status on pause but let one step run (if one is waiting)
		if (status != Status.FINISHED) {
			synchronized(paused) {
				this.stepToTime = time;
				status = Status.STEP ;
				paused.notifyAll();
			}
			synchronized (stepDone) {
				if (status == Status.PAUSE) return;
				try {
					stepDone.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public final void pause() {
		if (status != Status.FINISHED) {
			synchronized (updateFinished) {
				status = Status.PAUSE ;
			}
		}
	}

	@Override
	public final void play() {
		if (status != Status.FINISHED) {
			synchronized(paused) {
				status = Status.PLAY ;
				paused.notifyAll();
			}
		}
	}

	// for everything below here, I am not yet sure which of these need to be there. kai, mar'15

	Semaphore getAccessToQNetwork() {
		return accessToQNetwork;
	}

	// purely observational only below this line (probably not a problem)
	
	boolean isFinished() {
		return ( status == Status.FINISHED ) ;
	}

	public int getLocalTime() {
		return localTime;
	}

}
