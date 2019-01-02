/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public final class TaxiModule extends AbstractModule {

	private final AbstractQSimModule taxiQSimModule;

	@Inject
	private TaxiConfigGroup taxiCfg;

	public TaxiModule() {
		this(new TaxiQSimModule());
	}

	public TaxiModule(AbstractQSimModule taxiQSimModule) {
		this.taxiQSimModule = taxiQSimModule;
	}

	@Override
	public void install() {
		String mode = taxiCfg.getMode();
		bind(DvrpModes.key(Fleet.class, mode)).toProvider(new FleetProvider(taxiCfg.getTaxisFile())).asEagerSingleton();
		bind(Fleet.class).annotatedWith(Taxi.class).to(DvrpModes.key(Fleet.class, mode));

		bind(TravelDisutilityFactory.class).annotatedWith(Taxi.class)
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));

		bind(SubmittedTaxiRequestsCollector.class).asEagerSingleton();
		addControlerListenerBinding().to(SubmittedTaxiRequestsCollector.class);

		addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
		addControlerListenerBinding().to(TaxiStatsDumper.class);

		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		if (taxiCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
			// add more time profiles if necessary
		}

		bind(DvrpModes.key(PassengerRequestValidator.class, mode)).to(DefaultPassengerRequestValidator.class)
				.asEagerSingleton();

		installQSimModule(taxiQSimModule);
	}
}
