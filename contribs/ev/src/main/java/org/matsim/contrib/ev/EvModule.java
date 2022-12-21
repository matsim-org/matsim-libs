/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev;

import com.google.inject.Singleton;
import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.List;
import java.util.stream.Stream;

public class EvModule extends AbstractModule {
	public static final String EV_COMPONENT = "EV_COMPONENT";

	public EvModule(){}

	@Override
	public void install() {
		install( new EvBaseModule() );

		// this is not for DynVehicles.  Does that mean that we cannot combine charging for normal vehicles with charging for eTaxis?  Can't say ...  kai, dec'22
		installQSimModule(new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addMobsimScopeEventHandlerBinding().to( VehicleChargingHandler.class ).in( Singleton.class );
			}
		});

	}
}
