
/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayTravelTimeModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.withinday.trafficmonitoring;

import jakarta.inject.Singleton;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;

public class WithinDayTravelTimeModule extends AbstractModule {

	public WithinDayTravelTimeModule() {
	}

	@Override
	public void install() {
		if (getConfig().controller().getRoutingAlgorithmType() != ControllerConfigGroup.RoutingAlgorithmType.Dijkstra) {
			throw new RuntimeException(
					"for me, in KNAccidentScenario, this works with Dijkstra (default until spring 2019), and does not work with AStarLandmarks "
							+ "(default afterwards).  I have not tried the other routing options, nor have I systematically debugged. KN, feb'19");
		}

		bind(WithinDayTravelTime.class).in(Singleton.class);
		addEventHandlerBinding().to(WithinDayTravelTime.class);
		bindNetworkTravelTime().to(WithinDayTravelTime.class);
		// yyyyyy also needs to be bound as mobsim listener.  There is maybe a reason
		// why this is not added here, but could someone please explain?  thx.  kai, dec'17
		// Trying it out:
		addMobsimListenerBinding().to(WithinDayTravelTime.class);
	}
}
