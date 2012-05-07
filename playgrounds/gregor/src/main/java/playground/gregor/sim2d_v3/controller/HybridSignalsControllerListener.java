/* *********************************************************************** *
 * project: org.matsim.*
 * HybridSignalsControllerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.controller;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.controler.SignalsControllerListener;

/**
 * @author cdobler
 */
public class HybridSignalsControllerListener implements SignalsControllerListener, StartupListener, ShutdownListener, IterationStartsListener {

	private final List<SignalsControllerListener> listeners;
	
	public HybridSignalsControllerListener() {
		listeners = new ArrayList<SignalsControllerListener>();
	}
	
	public boolean addSignalsControllerListener(SignalsControllerListener listener) {
		return this.listeners.add(listener);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		for (SignalsControllerListener listener : listeners) {
			if (listener instanceof StartupListener) {
				((StartupListener) listener).notifyStartup(event);
			}
		}
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		for (SignalsControllerListener listener : listeners) {
			if (listener instanceof IterationStartsListener) {
				((IterationStartsListener) listener).notifyIterationStarts(event);
			}
		}
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		for (SignalsControllerListener listener : listeners) {
			if (listener instanceof ShutdownListener) {
				((ShutdownListener) listener).notifyShutdown(event);
			}
		}
	}	
}