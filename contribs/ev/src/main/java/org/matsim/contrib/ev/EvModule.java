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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.List;

public class EvModule extends AbstractModule {
	public static final String EV_COMPONENT = "EV_COMPONENT";

	@Override
	public void install() {
		install(new ElectricFleetModule());
		install(new ChargingInfrastructureModule());
		install(new ChargingModule());
		install(new DischargingModule());
		install(new EvStatsModule());

//		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( this.getConfig(), QSimComponentsConfigGroup.class );
//		List<String> cmps = qsimComponentsConfig.getActiveComponents();
//		cmps.add(  EvModule.EV_COMPONENT ) ;
//		qsimComponentsConfig.setActiveComponents( cmps );
//
//
//		addRoutingModuleBinding( TransportMode.car ).toProvider(new EvNetworkRoutingProvider(TransportMode.car) );
//		// (I assume that this is not on EvModule since one might want to evaluate if a standard route leads to an empty battery or not.  ???)
//
//		installQSimModule(new AbstractQSimModule() {
//			@Override protected void configureQSim() {
////						bind(VehicleChargingHandler.class).asEagerSingleton();
//				// this can be added to next line (does not need separate binding).
//
//				addMobsimScopeEventHandlerBinding().to( VehicleChargingHandler.class ).asEagerSingleton();
//				// (possibly not in EvModule because one wants to leave the decision to generate these events to the user??)
//				// (leaving this out fails the events equality)
//			}
//		});

	}
}
