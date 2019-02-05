/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetModule;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.FleetStatsCalculatorModule;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

/**
 * @author michalm
 */
public final class TaxiModeModule extends AbstractDvrpModeModule {
	private final TaxiConfigGroup taxiCfg;

	public TaxiModeModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	public void install() {
		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		addRoutingModuleBinding(getMode()).toInstance(new DynRoutingModule(getMode()));

		install(new FleetModule(getMode(), taxiCfg.getTaxisFile()));

		install(FleetStatsCalculatorModule.createModule(getMode(), TaxiStatsDumper.class,
				getter -> new TaxiStatsDumper(taxiCfg, getter.get(OutputDirectoryHierarchy.class),
						getter.get(IterationCounter.class))));

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(SubmittedTaxiRequestsCollector.class).to(SubmittedTaxiRequestsCollector.class)
						.asEagerSingleton();

				addModalQSimComponentBinding().toProvider(modalProvider(getter -> new TaxiSimulationConsistencyChecker(
						getter.getModal(SubmittedTaxiRequestsCollector.class), taxiCfg)));

				if (taxiCfg.getTimeProfiles()) {
					addModalQSimComponentBinding().toProvider(modalProvider(
							getter -> new TaxiStatusTimeProfileCollectorProvider(getter.getModal(Fleet.class),
									getter.get(MatsimServices.class),
									getter.getModal(SubmittedTaxiRequestsCollector.class), taxiCfg).get()));
				}

				bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class)
						.asEagerSingleton();
			}
		});
	}
}
