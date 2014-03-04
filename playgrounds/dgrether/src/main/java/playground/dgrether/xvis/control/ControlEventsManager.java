/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.control;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

import playground.dgrether.xvis.control.events.ScaleEvent;
import playground.dgrether.xvis.control.events.SelectionEvent;
import playground.dgrether.xvis.control.events.ShowPanelEvent;
import playground.dgrether.xvis.control.events.SignalGroupsSelectionEvent;
import playground.dgrether.xvis.control.events.SignalSystemSelectionEvent;
import playground.dgrether.xvis.control.events.TransformEvent;
import playground.dgrether.xvis.control.handlers.ControlEventListener;
import playground.dgrether.xvis.control.handlers.ScaleEventListener;
import playground.dgrether.xvis.control.handlers.ShowPanelEventListener;
import playground.dgrether.xvis.control.handlers.SignalGroupsSelectionEventListener;
import playground.dgrether.xvis.control.handlers.SignalSystemSelectionEventListener;
import playground.dgrether.xvis.control.handlers.TransformEventListener;


public class ControlEventsManager {
	
	private static final Logger log = Logger.getLogger(ControlEventsManager.class);
	
	private final EventListenerList listenerList = new EventListenerList();
	
	@SuppressWarnings("unchecked")
	public void addControlListener(final ControlEventListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (ControlEventListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.add(interfaces[i], l);
			}
		}
	}

	public void fireScaleEvent(ScaleEvent e) {
		ScaleEventListener[] listener =  this.listenerList.getListeners(ScaleEventListener.class);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].handleEvent(e);
    }
	}

	
	public void fireTransformEvent(TransformEvent e){
		TransformEventListener[] listener =  this.listenerList.getListeners(TransformEventListener.class);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].handleEvent(e);
    }
	}

	public void fireShowPanelEvent(ShowPanelEvent e) {
		ShowPanelEventListener[] listener =  this.listenerList.getListeners(ShowPanelEventListener.class);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].handleEvent(e);
    }
	}

	public void fireSelectionEvent(SelectionEvent e) {
		if (e instanceof SignalSystemSelectionEvent){
			SignalSystemSelectionEvent ev = (SignalSystemSelectionEvent)e;
			SignalSystemSelectionEventListener[] listener =  this.listenerList.getListeners(SignalSystemSelectionEventListener.class);
			for (int i = 0; i < listener.length; i++) {
				listener[i].handleEvent(ev);
			}
		}
		else if (e instanceof SignalGroupsSelectionEvent){
			SignalGroupsSelectionEvent ev = (SignalGroupsSelectionEvent)e;
			SignalGroupsSelectionEventListener[] listener =  this.listenerList.getListeners(SignalGroupsSelectionEventListener.class);
			for (int i = 0; i < listener.length; i++) {
				listener[i].handleEvent(ev);
			}
		}
		else {
			log.warn("Unknown selection event type: " + e.getClass());
		}

	}
	
	
}
