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
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.name.Names;

/**
 * @author michalm
 */
public final class TaxiModule extends AbstractModule {
	@Override
	public void install() {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(getConfig());
		bind(Fleet.class).toProvider(new FleetProvider(taxiCfg.getTaxisFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER))
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));
		
		bind(SubmittedTaxiRequestsCollector.class).toInstance(new SubmittedTaxiRequestsCollector());
		addControlerListenerBinding().to(SubmittedTaxiRequestsCollector.class);

		addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
		addControlerListenerBinding().to(TaxiStatsDumper.class);

		if (taxiCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
			// add more time profiles if necessary
		}
	}
}
