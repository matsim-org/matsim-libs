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
package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;

/**
 * @author dziemke
 */
final class MotorizedInteractionEngine implements MobsimBeforeSimStepListener {
	// ok to have this public final when ctor is package-private/injected: can only be used through injection

	private EventsManager eventsManager;
//	private List<Id<Link>> links;
//	private double startTime;
//	private double endTime;
//	private double frequency;

	@Inject
//	MotorizedInteractionEngine(EventsManager eventsManager, List<Id<Link>> links, double startTime, double endTime, double frequency) {
	MotorizedInteractionEngine(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
//		this.links = links;
//		this.startTime = startTime;
//		this.endTime = endTime;
//		this.frequency = frequency;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		double currentTime = e.getSimulationTime();
		double startTime = 8. * 60 * 60;
		double endTime = 12. * 60 * 60;
		double frequency = 3.;
		Id<Link> linkId = Id.createLinkId("6"); // The central link
		
		
		if ((currentTime % frequency == 0) && (currentTime >= startTime) && (currentTime <= endTime)) {
//			LOG.info("Current time = " + currentTime + " -- " + currentTime / 3600.);
//			for (Id<Link> linkId : links) {
				eventsManager.processEvent(new MotorizedInteractionEvent(e.getSimulationTime(), linkId, Id.create("evilCar", Vehicle.class)));
//			}
		}
	}
}
