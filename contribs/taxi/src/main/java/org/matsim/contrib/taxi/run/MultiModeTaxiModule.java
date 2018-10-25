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
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.data.validator.DefaultTaxiRequestValidator;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public final class MultiModeTaxiModule extends AbstractModule {
	private final TaxiConfigGroup taxiCfg;

	public MultiModeTaxiModule(TaxiConfigGroup taxiCfg) {
		this.taxiCfg = taxiCfg;
	}

	@Override
	public void install() {
		String mode = taxiCfg.getMode();
		Named namedMode = Names.named(mode);

		install(FleetProvider.createModule(mode, taxiCfg.getTaxisFileUrl(getConfig().getContext())));
		bind(Fleet.class).annotatedWith(Taxi.class).to(modalKey(Fleet.class)).asEagerSingleton();

		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER))
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));

		bind(modalKey(SubmittedTaxiRequestsCollector.class)).to(SubmittedTaxiRequestsCollector.class)
				.asEagerSingleton();
		addControlerListenerBinding().to(modalKey(SubmittedTaxiRequestsCollector.class));

		addControlerListenerBinding().toProvider(Providers.createProvider(
				injector -> new TaxiSimulationConsistencyChecker(
						injector.getInstance(modalKey(SubmittedTaxiRequestsCollector.class)), taxiCfg)));
		addControlerListenerBinding().toProvider(Providers.createProvider(
				injector -> new TaxiStatsDumper(injector.getInstance(modalKey(Fleet.class)), taxiCfg,
						injector.getInstance(OutputDirectoryHierarchy.class))));

		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		if (taxiCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(Providers.createProvider(
					injector -> new TaxiStatusTimeProfileCollectorProvider(injector.getInstance(modalKey(Fleet.class)),
							injector.getInstance(MatsimServices.class),
							injector.getInstance(modalKey(SubmittedTaxiRequestsCollector.class))).get()));
			// add more time profiles if necessary
		}

		bind(modalKey(TaxiRequestValidator.class)).to(DefaultTaxiRequestValidator.class).asEagerSingleton();
	}

	private <T> Key<T> modalKey(Class<T> type) {
		return Key.get(type, Names.named(taxiCfg.getMode()));
	}
}
