/* *********************************************************************** *
 * project: org.matsim.*
 * FixedOrderQueueSimulationListener.java
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

package org.matsim.core.mobsim.framework.listeners;

import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * To avoid problems with the order of some SimulationListeners, this
 * class can be used execute them in a fixed order. Even something like 
 * ParallelListenerHandling (which is not available yet) should not able 
 * to break the order.
 * 
 * @author cdobler
 */
public class FixedOrderSimulationListener implements MobsimInitializedListener,
	MobsimBeforeSimStepListener, MobsimAfterSimStepListener,
	MobsimBeforeCleanupListener {

	List<MobsimInitializedListener> simulationInitializedListener;
	List<MobsimBeforeSimStepListener> simulationBeforeSimStepListener;
	List<MobsimAfterSimStepListener> simulationAfterSimStepListener;
	List<MobsimBeforeCleanupListener> simulationBeforeCleanupListener;

	public FixedOrderSimulationListener() {
		simulationInitializedListener = new ArrayList<MobsimInitializedListener>();
		simulationBeforeSimStepListener = new ArrayList<MobsimBeforeSimStepListener>();
		simulationAfterSimStepListener = new ArrayList<MobsimAfterSimStepListener>();
		simulationBeforeCleanupListener = new ArrayList<MobsimBeforeCleanupListener>();
	}

	/**
	 * Adds the SimulationListener to all ListenerLists that it supports.
	 */
	public void addSimulationListener(MobsimListener listener) {
		if (listener instanceof MobsimInitializedListener) {
			addSimulationInitializedListener((MobsimInitializedListener) listener);
		}
		if (listener instanceof MobsimBeforeSimStepListener) {
			addSimulationBeforeSimStepListener((MobsimBeforeSimStepListener) listener);
		}
		if (listener instanceof MobsimAfterSimStepListener) {
			addSimulationAfterSimStepListener((MobsimAfterSimStepListener) listener);
		}
		if (listener instanceof MobsimBeforeCleanupListener) {
			addSimulationBeforeCleanupListener((MobsimBeforeCleanupListener) listener);
		}
	}
	
	/**
	 * Removes the SimulationListener from all ListenerLists that it supports.
	 */
	public void removeSimulationListener(MobsimListener listener) {
		if (listener instanceof MobsimInitializedListener) {
			removeSimulationInitializedListener((MobsimInitializedListener) listener);
		}
		if (listener instanceof MobsimBeforeSimStepListener) {
			removeSimulationBeforeSimStepListener((MobsimBeforeSimStepListener) listener);
		}
		if (listener instanceof MobsimAfterSimStepListener) {
			removeSimulationAfterSimStepListener((MobsimAfterSimStepListener) listener);
		}
		if (listener instanceof MobsimBeforeCleanupListener) {
			removeSimulationBeforeCleanupListener((MobsimBeforeCleanupListener) listener);
		}
	}
	
	public void addSimulationInitializedListener(MobsimInitializedListener listener) {
		simulationInitializedListener.add(listener);
	}

	public void removeSimulationInitializedListener(MobsimInitializedListener listener) {
		simulationInitializedListener.remove(listener);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for(MobsimInitializedListener listener : simulationInitializedListener) {
			listener.notifyMobsimInitialized(e);
		}
	}

	public void addSimulationBeforeSimStepListener(MobsimBeforeSimStepListener listener) {
		simulationBeforeSimStepListener.add(listener);
	}

	public void removeSimulationBeforeSimStepListener(MobsimBeforeSimStepListener listener) {
		simulationBeforeSimStepListener.remove(listener);
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		for(MobsimBeforeSimStepListener listener : simulationBeforeSimStepListener) {
			listener.notifyMobsimBeforeSimStep(e);
		}
	}

	public void addSimulationAfterSimStepListener(MobsimAfterSimStepListener listener) {
		simulationAfterSimStepListener.add(listener);
	}

	public void removeSimulationAfterSimStepListener(MobsimAfterSimStepListener listener) {
		simulationAfterSimStepListener.remove(listener);
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		for(MobsimAfterSimStepListener listener : simulationAfterSimStepListener) {
			listener.notifyMobsimAfterSimStep(e);
		}
	}

	public void addSimulationBeforeCleanupListener(MobsimBeforeCleanupListener listener) {
		simulationBeforeCleanupListener.add(listener);
	}

	public void removeSimulationBeforeCleanupListener(MobsimBeforeCleanupListener listener) {
		simulationBeforeCleanupListener.remove(listener);
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		for(MobsimBeforeCleanupListener listener : simulationBeforeCleanupListener) {
			listener.notifyMobsimBeforeCleanup(e);
		}
	}

}