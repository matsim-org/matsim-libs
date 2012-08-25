/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.eMob.controllers;

import org.matsim.contrib.eMob.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;

public class EMobInductiveChargingController extends Controler {

	public EMobInductiveChargingController(String[] args) {
		super(args);
	}

	public static void main(String[] args) {
		EMobInductiveChargingController eMobInductiveChargingController = new EMobInductiveChargingController(args);
		
		eMobInductiveChargingController.addControlerListener(new StartupListener() {
			
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(new InductiveStreetCharger());
			}
		});
		
		eMobInductiveChargingController.run();
		
		
		
	}
	
}
