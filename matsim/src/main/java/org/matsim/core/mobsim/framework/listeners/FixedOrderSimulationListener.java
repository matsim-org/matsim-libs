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

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;

/**
 * To avoid problems with the order of some SimulationListeners, this
 * class can be used execute them in a fixed order. Even something like 
 * ParallelListenerHandling (which is not available yet) should not able 
 * to break the order.
 * 
 * @author cdobler
 */
public class FixedOrderSimulationListener implements SimulationInitializedListener,
	SimulationBeforeSimStepListener, SimulationAfterSimStepListener,
	SimulationBeforeCleanupListener {

	List<SimulationInitializedListener> simulationInitializedListener;
	List<SimulationBeforeSimStepListener> simulationBeforeSimStepListener;
	List<SimulationAfterSimStepListener> simulationAfterSimStepListener;
	List<SimulationBeforeCleanupListener> simulationBeforeCleanupListener;

	public FixedOrderSimulationListener() {
		simulationInitializedListener = new ArrayList<SimulationInitializedListener>();
		simulationBeforeSimStepListener = new ArrayList<SimulationBeforeSimStepListener>();
		simulationAfterSimStepListener = new ArrayList<SimulationAfterSimStepListener>();
		simulationBeforeCleanupListener = new ArrayList<SimulationBeforeCleanupListener>();
	}

	/**
	 * Adds the SimulationListener to all ListenerLists that it supports.
	 */
	public void addSimulationListener(SimulationListener listener) {
		if (listener instanceof SimulationInitializedListener) {
			addSimulationInitializedListener((SimulationInitializedListener) listener);
		}
		if (listener instanceof SimulationBeforeSimStepListener) {
			addSimulationBeforeSimStepListener((SimulationBeforeSimStepListener) listener);
		}
		if (listener instanceof SimulationAfterSimStepListener) {
			addSimulationAfterSimStepListener((SimulationAfterSimStepListener) listener);
		}
		if (listener instanceof SimulationBeforeCleanupListener) {
			addSimulationBeforeCleanupListener((SimulationBeforeCleanupListener) listener);
		}
	}
	
	/**
	 * Removes the SimulationListener from all ListenerLists that it supports.
	 */
	public void removeSimulationListener(SimulationListener listener) {
		if (listener instanceof SimulationInitializedListener) {
			removeSimulationInitializedListener((SimulationInitializedListener) listener);
		}
		if (listener instanceof SimulationBeforeSimStepListener) {
			removeSimulationBeforeSimStepListener((SimulationBeforeSimStepListener) listener);
		}
		if (listener instanceof SimulationAfterSimStepListener) {
			removeSimulationAfterSimStepListener((SimulationAfterSimStepListener) listener);
		}
		if (listener instanceof SimulationBeforeCleanupListener) {
			removeSimulationBeforeCleanupListener((SimulationBeforeCleanupListener) listener);
		}
	}
	
	public void addSimulationInitializedListener(SimulationInitializedListener listener) {
		simulationInitializedListener.add(listener);
	}

	public void removeSimulationInitializedListener(SimulationInitializedListener listener) {
		simulationInitializedListener.remove(listener);
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		for(SimulationInitializedListener listener : simulationInitializedListener) {
			listener.notifySimulationInitialized(e);
		}
	}

	public void addSimulationBeforeSimStepListener(SimulationBeforeSimStepListener listener) {
		simulationBeforeSimStepListener.add(listener);
	}

	public void removeSimulationBeforeSimStepListener(SimulationBeforeSimStepListener listener) {
		simulationBeforeSimStepListener.remove(listener);
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		for(SimulationBeforeSimStepListener listener : simulationBeforeSimStepListener) {
			listener.notifySimulationBeforeSimStep(e);
		}
	}

	public void addSimulationAfterSimStepListener(SimulationAfterSimStepListener listener) {
		simulationAfterSimStepListener.add(listener);
	}

	public void removeSimulationAfterSimStepListener(SimulationAfterSimStepListener listener) {
		simulationAfterSimStepListener.remove(listener);
	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		for(SimulationAfterSimStepListener listener : simulationAfterSimStepListener) {
			listener.notifySimulationAfterSimStep(e);
		}
	}

	public void addSimulationBeforeCleanupListener(SimulationBeforeCleanupListener listener) {
		simulationBeforeCleanupListener.add(listener);
	}

	public void removeSimulationBeforeCleanupListener(SimulationBeforeCleanupListener listener) {
		simulationBeforeCleanupListener.remove(listener);
	}

	@Override
	public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent e) {
		for(SimulationBeforeCleanupListener listener : simulationBeforeCleanupListener) {
			listener.notifySimulationBeforeCleanup(e);
		}
	}

}