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

package org.matsim.contrib.ev.stats;

import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingEventSequenceCollector;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EvStatsModule extends AbstractModule {
	@Inject
	private EvConfigGroup evCfg;

	@Override
	public void install() {
		bind(ChargingEventSequenceCollector.class).asEagerSingleton();
		addEventHandlerBinding().to(ChargingEventSequenceCollector.class);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				if (evCfg.timeProfiles) {
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(
							SocHistogramTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(
							IndividualChargeTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(
							ChargerOccupancyTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(ChargerOccupancyXYDataProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(
							VehicleTypeAggregatedChargeTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).to(EvMobsimListener.class);

					bind(ChargerPowerCollector.class).asEagerSingleton();
					addMobsimScopeEventHandlerBinding().to(ChargerPowerCollector.class);
					// add more time profiles if necessary
				}
			}
		});
	}
}
