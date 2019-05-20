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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DischargingModule extends AbstractModule {
	private final EvConfigGroup evCfg;

	private static DriveEnergyConsumption.Factory DEFAULT_DRIVE_CONSUMPTION_FACTORY //
			= ev -> new OhdeSlaskiDriveEnergyConsumption();

	// TODO fixed temperature 15 oC
	// FIXME reintroduce TemperatureProvider
	private static AuxEnergyConsumption.Factory DEFAULT_AUX_CONSUMPTION_FACTORY //
			= ev -> new OhdeSlaskiAuxEnergyConsumption(ev, () -> 15, (v, t) -> true);

	private final DriveEnergyConsumption.Factory driveConsumptionFactory;
	private final AuxEnergyConsumption.Factory auxConsumptionFactory;

	public DischargingModule(EvConfigGroup evCfg) {
		this(evCfg, DEFAULT_DRIVE_CONSUMPTION_FACTORY, DEFAULT_AUX_CONSUMPTION_FACTORY);
	}

	public DischargingModule(EvConfigGroup evCfg, DriveEnergyConsumption.Factory driveConsumptionFactory,
			AuxEnergyConsumption.Factory auxConsumptionFactory) {
		this.evCfg = evCfg;
		this.driveConsumptionFactory = driveConsumptionFactory;
		this.auxConsumptionFactory = auxConsumptionFactory;
	}

	@Override
	public void install() {
		//XXX "isTurnedOn" returns true ==> should not be used when for "seperateAuxDischargingHandler"
		boolean isSeperateAuxDischargingHandler = evCfg.getAuxDischargingSimulation()
				== EvConfigGroup.AuxDischargingSimulation.seperateAuxDischargingHandler;

		bind(DriveEnergyConsumption.Factory.class).toInstance(driveConsumptionFactory);
		if (isSeperateAuxDischargingHandler) {
			bind(AuxEnergyConsumption.Factory.class).toInstance(auxConsumptionFactory);
		}

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(DriveDischargingHandler.class).asEagerSingleton();
				if (isSeperateAuxDischargingHandler) {
					bind(AuxDischargingHandler.class).asEagerSingleton();
					addQSimComponentBinding(EvModule.EV_COMPONENT).to(AuxDischargingHandler.class);
				}
			}
		});
	}
}
