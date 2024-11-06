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

 import org.matsim.contrib.ev.EvConfigGroup;
 import org.matsim.contrib.ev.infrastructure.Charger;
 import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
 import org.matsim.core.mobsim.qsim.InternalInterface;
 import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
 
 import com.google.inject.Inject;
 
 public class ChargingHandler implements MobsimEngine {
	 private final Iterable<Charger> chargers;
	 private final int chargeTimeStep;
 
	 @Inject
	 ChargingHandler(ChargingInfrastructure chargingInfrastructure, EvConfigGroup evConfig) {
		 this.chargers = chargingInfrastructure.getChargers().values();
		 this.chargeTimeStep = evConfig.chargeTimeStep;
	 }
 
	 @Override
	 public void doSimStep(double time) {
		 if (time % chargeTimeStep == 0) {
			 for (Charger c : chargers) {
				 c.getLogic().chargeVehicles(chargeTimeStep, time);
			 }
		 }
	 }
 
	 @Override
	 public void onPrepareSim() {
		 // empty
	 }
 
	 @Override
	 public void afterSim() {
		 // empty
	 }
 
	 @Override
	 public void setInternalInterface(InternalInterface internalInterface) {
		 // empty
	 }
 }
 