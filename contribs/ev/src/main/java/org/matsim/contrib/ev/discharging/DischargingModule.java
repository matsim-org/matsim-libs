/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.discharging;

import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class DischargingModule extends AbstractModule {
	@Override
	public void install() {
		bind(DriveEnergyConsumption.Factory.class).toInstance(ev -> new OhdeSlaskiDriveEnergyConsumption());
		bind(TemperatureService.class).toInstance(linkId -> 15);// XXX fixed temperature 15 oC
		bind(AuxEnergyConsumption.Factory.class).to(OhdeSlaskiAuxEnergyConsumption.Factory.class).in(Singleton.class);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(DriveDischargingHandler.class).in(Singleton.class);
				addMobsimScopeEventHandlerBinding().to(DriveDischargingHandler.class);
				this.addQSimComponentBinding(EvModule.EV_COMPONENT).to(DriveDischargingHandler.class);
				// event handlers are not qsim components

				this.bind(IdleDischargingHandler.class).in(Singleton.class);
				addMobsimScopeEventHandlerBinding().to(IdleDischargingHandler.class);
				this.addQSimComponentBinding(EvModule.EV_COMPONENT).to(IdleDischargingHandler.class);

				//by default, no vehicle will be AUX-discharged when not moving
				this.bind(IdleDischargingHandler.VehicleProvider.class).toInstance(event -> null);
			}
		});
	}
}
