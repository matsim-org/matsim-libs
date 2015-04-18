/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MobsimListenerManager.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.sergioo.ptsim2013;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.utils.misc.ClassUtils;

import javax.swing.event.EventListenerList;

/**
 * Helper for the QueueSimulation to manage their listeners and fire
 * QueueSimulationEvents.
 *
 * @author dgrether
 */
class MobsimListenerManager implements MatsimManager {

	private final static Logger log = Logger.getLogger(MobsimListenerManager.class);

	private final RunnableMobsim sim;

	private final EventListenerList listenerList = new EventListenerList();

	public MobsimListenerManager(RunnableMobsim sim){
		this.sim = sim;
	}

	@SuppressWarnings("unchecked")
	public void addQueueSimulationListener(final MobsimListener l) {
		log.info("calling addQueueSimulationListener");
		for (Class interfaceClass : ClassUtils.getAllTypes(l.getClass())) {
			if (MobsimListener.class.isAssignableFrom(interfaceClass)) {
				this.listenerList.add(interfaceClass, l);
				log.info("  assigned class " + MobsimListener.class.getName() + " to interface " + interfaceClass.getName());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void removeQueueSimulationListener(final MobsimListener l) {
		for (Class interfaceClass : ClassUtils.getAllTypes(l.getClass())) {
			if (MobsimListener.class.isAssignableFrom(interfaceClass)) {
				this.listenerList.remove(interfaceClass, l);
			}
		}
	}

	/**
	 * Creates the event and notifies all listeners
	 */
	public void fireQueueSimulationInitializedEvent() {
		MobsimInitializedEvent<RunnableMobsim> event = new MobsimInitializedEvent<RunnableMobsim>(sim);
		MobsimInitializedListener[] listener = this.listenerList.getListeners(MobsimInitializedListener.class);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyMobsimInitialized(event);
    }
	}

	/**
	 * Creates the event and notifies all listeners
	 *
	 * @param simTime the current time in the simulation
	 */
	public void fireQueueSimulationAfterSimStepEvent(final double simTime) {
		MobsimAfterSimStepEvent<RunnableMobsim> event = new MobsimAfterSimStepEvent<RunnableMobsim>(sim, simTime);
		MobsimAfterSimStepListener[] listener = this.listenerList.getListeners(MobsimAfterSimStepListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyMobsimAfterSimStep(event);
		}
	}

	/**
	 * Creates the event and notifies all listeners
	 */
	public void fireQueueSimulationBeforeCleanupEvent(){
		MobsimBeforeCleanupEvent<RunnableMobsim> event = new MobsimBeforeCleanupEvent<RunnableMobsim>(this.sim);
		MobsimBeforeCleanupListener[] listener = this.listenerList.getListeners(MobsimBeforeCleanupListener.class);
		for (int i = 0; i < listener.length; i++){
			listener[i].notifyMobsimBeforeCleanup(event);
		}
	}

	public void fireQueueSimulationBeforeSimStepEvent(double time) {
		MobsimBeforeSimStepEvent<RunnableMobsim> event = new MobsimBeforeSimStepEvent<RunnableMobsim>(sim, time);
		MobsimBeforeSimStepListener[] listener = this.listenerList.getListeners(MobsimBeforeSimStepListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyMobsimBeforeSimStep(event);
		}
	}

}
