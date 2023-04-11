/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import com.google.inject.Inject;

public class ChargingHandler implements MobsimAfterSimStepListener {
	private static final Logger log = LogManager.getLogger( ChargingHandler.class );
	private final Iterable<Charger> chargers;
	private final int chargeTimeStep;

	@Inject
	ChargingHandler(ChargingInfrastructure chargingInfrastructure, EvConfigGroup evConfig) {
		this.chargers = chargingInfrastructure.getChargers().values();
		this.chargeTimeStep = evConfig.chargeTimeStep;
	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		if ((e.getSimulationTime() + 1) % chargeTimeStep == 0) {
			for (Charger c : chargers) {
				c.getLogic().chargeVehicles(chargeTimeStep, e.getSimulationTime());
			}
		}
	}
}
