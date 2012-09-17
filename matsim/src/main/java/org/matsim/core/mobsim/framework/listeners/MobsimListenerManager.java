/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimListenerManager
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEventImpl;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEventImpl;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEventImpl;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEventImpl;
import org.matsim.core.utils.misc.ClassUtils;

/**
 * Helper for the QueueSimulation to manage their listeners and fire
 * QueueSimulationEvents.
 *
 * @author dgrether
 */
public class MobsimListenerManager implements MatsimManager {

	private final static Logger log = Logger.getLogger(MobsimListenerManager.class);

	private final Mobsim sim;

	private final EventListenerList listenerList = new EventListenerList();

	public MobsimListenerManager(Mobsim sim){
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
		MobsimInitializedEvent<Mobsim> event = new MobsimInitializedEventImpl<Mobsim>(sim);
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
		MobsimAfterSimStepEvent<Mobsim> event = new MobsimAfterSimStepEventImpl<Mobsim>(sim, simTime);
		MobsimAfterSimStepListener[] listener = this.listenerList.getListeners(MobsimAfterSimStepListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyMobsimAfterSimStep(event);
		}
	}

	/**
	 * Creates the event and notifies all listeners
	 */
	public void fireQueueSimulationBeforeCleanupEvent(){
		MobsimBeforeCleanupEvent<Mobsim> event = new MobsimBeforeCleanupEventImpl<Mobsim>(this.sim);
		MobsimBeforeCleanupListener[] listener = this.listenerList.getListeners(MobsimBeforeCleanupListener.class);
		for (int i = 0; i < listener.length; i++){
			listener[i].notifyMobsimBeforeCleanup(event);
		}
	}

	public void fireQueueSimulationBeforeSimStepEvent(double time) {
		MobsimBeforeSimStepEvent<Mobsim> event = new MobsimBeforeSimStepEventImpl<Mobsim>(sim, time);
		MobsimBeforeSimStepListener[] listener = this.listenerList.getListeners(MobsimBeforeSimStepListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyMobsimBeforeSimStep(event);
		}
	}

}
