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
package org.matsim.mobsim.queuesim.listener;

import javax.swing.event.EventListenerList;

import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.events.QueueSimulationBeforeCleanupEvent;
import org.matsim.mobsim.queuesim.events.QueueSimulationBeforeCleanupEventImpl;
import org.matsim.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.mobsim.queuesim.events.QueueSimulationInitializedEventImpl;

/**
 * Helper for the QueueSimulation to manage their listeners and fire
 * QueueSimulationEvents.
 * @author dgrether
 *
 */
public class QueueSimListenerManager {
	
	private QueueSimulation queuesim;
	
	private final EventListenerList listenerList = new EventListenerList();
	
	
	public QueueSimListenerManager(QueueSimulation qsim){
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
	 */
	public void fireQueueSimulationBeforeClenupEvent(){
		QueueSimulationBeforeCleanupEvent event = new QueueSimulationBeforeCleanupEventImpl(this.queuesim);
		QueueSimulationBeforeCleanupListener[] listener = this.listenerList.getListeners(QueueSimulationBeforeCleanupListener.class);
		for (int i = 0; i < listener.length; i++){
			listener[i].notifySimulationBeforeCleanup(event);
		}
	}
	

}
