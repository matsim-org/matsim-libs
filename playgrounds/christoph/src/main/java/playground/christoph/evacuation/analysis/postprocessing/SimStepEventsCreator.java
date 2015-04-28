/* *********************************************************************** *
 * project: org.matsim.*
 * SimStepEventsCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis.postprocessing;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import java.util.List;

/**
 * 
 * When parsing an events file, MobsimEvents are not created. However, some
 * EventHandlers need them to run as expected. Therefore, this class creates them
 * for a list of given MobsimListeners.
 * 
 * @author cdobler
 */
public class SimStepEventsCreator implements BasicEventHandler {
	
	private final List<MobsimListener> mobsimListeners;
	private double lastSimStep = 0.0; 
	
	public SimStepEventsCreator(List<MobsimListener> mobsimListeners) {
		this.mobsimListeners = mobsimListeners;
		
		MobsimInitializedEvent<Mobsim> eInitialized = new MobsimInitializedEvent<Mobsim>(null);
		for(MobsimListener mobsimListener : mobsimListeners) {
			if (mobsimListener instanceof BeforeMobsimListener) {
				((MobsimInitializedListener) mobsimListener).notifyMobsimInitialized(eInitialized);
			}
		}
		
		MobsimBeforeSimStepEvent<Mobsim> eBefore = new MobsimBeforeSimStepEvent<Mobsim>(null, lastSimStep);
		for(MobsimListener mobsimListener : mobsimListeners) {
			if (mobsimListener instanceof MobsimBeforeSimStepListener) {
				((MobsimBeforeSimStepListener) mobsimListener).notifyMobsimBeforeSimStep(eBefore);
			}
		}
	}
	
	@Override
	public void handleEvent(Event event) {
		double time = event.getTime();
		while (time > lastSimStep) {
			
			MobsimAfterSimStepEvent<Mobsim> eAfter = new MobsimAfterSimStepEvent<Mobsim>(null, lastSimStep);
			for(MobsimListener mobsimListener : mobsimListeners) {
				if (mobsimListener instanceof MobsimAfterSimStepListener) {
					((MobsimAfterSimStepListener) mobsimListener).notifyMobsimAfterSimStep(eAfter);
				}
			}
			
			lastSimStep++;
			
			MobsimBeforeSimStepEvent<Mobsim> eBefore = new MobsimBeforeSimStepEvent<Mobsim>(null, lastSimStep);
			for(MobsimListener mobsimListener : mobsimListeners) {
				if (mobsimListener instanceof BeforeMobsimListener) {
					((MobsimBeforeSimStepListener) mobsimListener).notifyMobsimBeforeSimStep(eBefore);
				}
			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		// nothing to do here
	}
	
	public void allEventsProcessed() {
		MobsimAfterSimStepEvent<Mobsim> e = new MobsimAfterSimStepEvent<Mobsim>(null, lastSimStep);
		for(MobsimListener mobsimListener : mobsimListeners) {
			if (mobsimListener instanceof MobsimAfterSimStepListener) {
				((MobsimAfterSimStepListener) mobsimListener).notifyMobsimAfterSimStep(e);
			}
		}
	}
}