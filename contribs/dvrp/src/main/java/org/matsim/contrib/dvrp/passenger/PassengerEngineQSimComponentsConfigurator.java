/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public class PassengerEngineQSimComponentsConfigurator implements QSimComponentsConfigurator {
	private final String mode;

	public PassengerEngineQSimComponentsConfigurator(String mode) {
		this.mode = mode;
	}

	public void configure(QSimComponents components) {
		components.activeMobsimEngines.add(PassengerEngineQSimModule.PASSENGER_ENGINE_NAME_PREFIX + mode);
		components.activeDepartureHandlers.add(PassengerEngineQSimModule.PASSENGER_ENGINE_NAME_PREFIX + mode);
	}
}
