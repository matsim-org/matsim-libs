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

import org.matsim.contrib.ev.EvConfigGroup;
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
		EvConfigGroup evConfig = EvConfigGroup.get(getConfig());

		switch (evConfig.getDriveEnergyConsumption()) {
			case OhdeSlaski:
				bind(DriveEnergyConsumption.Factory.class).toInstance(ev -> new OhdeSlaskiDriveEnergyConsumption());
				break;
			case AttributeBased:
				bind(DriveEnergyConsumption.Factory.class).toInstance(new AttributeBasedDriveEnergyConsumption.Factory());
				break;
			case None:
				bind(DriveEnergyConsumption.Factory.class).toInstance(ev -> (link, travelTime, enterTime) -> 0.0);
		}

		switch (evConfig.getAuxEnergyConsumption()) {
			case OhdeSlaski:
				bind(AuxEnergyConsumption.Factory.class).to(OhdeSlaskiAuxEnergyConsumption.Factory.class).in(Singleton.class);
				break;
			case AttributeBased:
				bind(AuxEnergyConsumption.Factory.class).toInstance(new AttributeBasedAuxEnergyConsumption.Factory());
				break;
			case None:
				bind(AuxEnergyConsumption.Factory.class).toInstance(ev -> (beginTime, duration, linkId) -> 0.0);
		}

		bind(TemperatureService.class).toInstance(linkId -> 15);// XXX fixed temperature 15 oC

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
