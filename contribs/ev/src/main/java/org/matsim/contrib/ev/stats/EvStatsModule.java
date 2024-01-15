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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingEventSequenceCollector;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

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
		addControlerListenerBinding().to(ChargingProceduresCSVWriter.class).in(Singleton.class);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				if (evCfg.timeProfiles) {
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(SocHistogramTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(IndividualChargeTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(ChargerOccupancyTimeProfileCollectorProvider.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).to(ChargerOccupancyXYDataCollector.class).asEagerSingleton();
					addQSimComponentBinding(EvModule.EV_COMPONENT).toProvider(VehicleTypeAggregatedChargeTimeProfileCollectorProvider.class);

					bind(EnergyConsumptionCollector.class).asEagerSingleton();
					addMobsimScopeEventHandlerBinding().to(EnergyConsumptionCollector.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).to(EnergyConsumptionCollector.class);
					// add more time profiles if necessary
				}
			}
		});
		bind(ChargerPowerTimeProfileCalculator.class).asEagerSingleton();
		addEventHandlerBinding().to(ChargerPowerTimeProfileCalculator.class);
		addControlerListenerBinding().toProvider(new Provider<>() {
			@Inject
			private ChargerPowerTimeProfileCalculator calculator;
			@Inject
			private MatsimServices matsimServices;

			@Override
			public ControlerListener get() {
				var profileView = new ChargerPowerTimeProfileView(calculator);
				return new ProfileWriter(matsimServices,"ev",profileView,"charger_power_time_profiles");

			}
		});
	}
}
