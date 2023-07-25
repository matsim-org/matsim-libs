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

import jakarta.inject.Inject;

/**
 * @author dziemke
 * @deprecated -- it might be possible to repair this, but as of now it is not working.  kai, nov'22
 */
@Deprecated
final class MotorizedInteractionEngineForATest implements MobsimBeforeSimStepListener {

	private final EventsManager eventsManager;
	@Inject MotorizedInteractionEngineForATest( EventsManager eventsManager ) {
		this.eventsManager = eventsManager;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		double currentTime = e.getSimulationTime();
		double startTime = 8. * 60 * 60;
		double endTime = 12. * 60 * 60;
		double frequency = 3.;
		Id<Link> linkId = Id.createLinkId("6"); // The central link

		// this generates motorized interaction events with a certain frequency, but without looking at actual cars.
		if ((currentTime % frequency == 0) && (currentTime >= startTime) && (currentTime <= endTime)) {
			eventsManager.processEvent(new MotorizedInteractionEvent(e.getSimulationTime(), linkId, Id.create("evilCar", Vehicle.class)));
		}
	}
}
