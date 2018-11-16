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
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.data.validator.DefaultTaxiRequestValidator;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Key;

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
		bind(DvrpModes.key(Fleet.class, mode)).toProvider(new FleetProvider(taxiCfg.getTaxisFile())).asEagerSingleton();

		bind(modalKey(SubmittedTaxiRequestsCollector.class)).to(SubmittedTaxiRequestsCollector.class)
				.asEagerSingleton();
		addControlerListenerBinding().to(modalKey(SubmittedTaxiRequestsCollector.class));

		addControlerListenerBinding().toProvider(ModalProviders.createProvider(mode,
				getter -> new TaxiSimulationConsistencyChecker(getter.getModal(SubmittedTaxiRequestsCollector.class),
						taxiCfg)));

		addControlerListenerBinding().toProvider(ModalProviders.createProvider(mode,
				getter -> new TaxiStatsDumper(getter.getModal(Fleet.class), taxiCfg,
						getter.get(OutputDirectoryHierarchy.class))));

		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		if (taxiCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(ModalProviders.createProvider(mode,
					getter -> new TaxiStatusTimeProfileCollectorProvider(getter.getModal(Fleet.class),
							getter.get(MatsimServices.class), getter.getModal(SubmittedTaxiRequestsCollector.class),
							taxiCfg).get()));
			// add more time profiles if necessary
		}

		bind(modalKey(TaxiRequestValidator.class)).to(DefaultTaxiRequestValidator.class).asEagerSingleton();
	}

	private <T> Key<T> modalKey(Class<T> type) {
		return DvrpModes.key(type, taxiCfg.getMode());
	}
}
