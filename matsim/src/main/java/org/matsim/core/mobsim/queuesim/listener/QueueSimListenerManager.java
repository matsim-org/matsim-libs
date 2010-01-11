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

package org.matsim.core.mobsim.queuesim.listener;

import javax.swing.event.EventListenerList;

import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEventImpl;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeCleanupEventImpl;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeSimStepEventImpl;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEventImpl;

/**
 * Helper for the QueueSimulation to manage their listeners and fire
 * QueueSimulationEvents.
 *
 * @author dgrether
 */
public class QueueSimListenerManager<T extends Simulation> {
	
	private final T queuesim;

	private final EventListenerList listenerList = new EventListenerList();

	public QueueSimListenerManager(T qsim){
		this.queuesim = qsim;
	}
	
	@SuppressWarnings("unchecked")
	public void addQueueSimulationListener(final QueueSimulationListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (QueueSimulationListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.add(interfaces[i], l);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void removeQueueSimulationListener(final QueueSimulationListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (QueueSimulationListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.remove(interfaces[i], l);
			}
		}
	}

	/**
	 * Creates the event and notifies all listeners
	 */
	public void fireQueueSimulationInitializedEvent() {
		QueueSimulationInitializedEvent event = new QueueSimulationInitializedEventImpl(queuesim);
		QueueSimulationInitializedListener[] listener = this.listenerList.getListeners(QueueSimulationInitializedListener.class);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifySimulationInitialized(event);
    }
	}

	/**
	 * Creates the event and notifies all listeners
	 * 
	 * @param simTime the current time in the simulation
	 */
	public void fireQueueSimulationAfterSimStepEvent(final double simTime) {
		QueueSimulationAfterSimStepEvent event = new QueueSimulationAfterSimStepEventImpl(queuesim, simTime);
		QueueSimulationAfterSimStepListener[] listener = this.listenerList.getListeners(QueueSimulationAfterSimStepListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifySimulationAfterSimStep(event);
		}
	}

	/**
	 * Creates the event and notifies all listeners
	 */
	public void fireQueueSimulationBeforeCleanupEvent(){
		QueueSimulationBeforeCleanupEvent event = new QueueSimulationBeforeCleanupEventImpl(this.queuesim);
		QueueSimulationBeforeCleanupListener[] listener = this.listenerList.getListeners(QueueSimulationBeforeCleanupListener.class);
		for (int i = 0; i < listener.length; i++){
			listener[i].notifySimulationBeforeCleanup(event);
		}
	}

	public void fireQueueSimulationBeforeSimStepEvent(double time) {
		QueueSimulationBeforeSimStepEvent event = new QueueSimulationBeforeSimStepEventImpl(queuesim, time);
		QueueSimulationBeforeSimStepListener[] listener = this.listenerList.getListeners(QueueSimulationBeforeSimStepListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifySimulationBeforeSimStep(event);
		}
	}

}
