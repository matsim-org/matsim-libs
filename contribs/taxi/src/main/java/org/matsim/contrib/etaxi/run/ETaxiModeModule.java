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

package org.matsim.contrib.etaxi.run;

import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.QSimScopeObjectListenerModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.ev.dvrp.EvDvrpFleetModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

/**
 * @author michalm
 */
public final class ETaxiModeModule extends AbstractDvrpModeModule {
	private final TaxiConfigGroup taxiCfg;

	public ETaxiModeModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());

		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		addRoutingModuleBinding(getMode()).toInstance(new DynRoutingModule(getMode()));

		install(new EvDvrpFleetModule(getMode(), taxiCfg.getTaxisFile()));

		install(QSimScopeObjectListenerModule.builder(TaxiStatsDumper.class)
				.mode(getMode())
				.objectClass(Fleet.class)
				.listenerCreator(getter -> new TaxiStatsDumper(taxiCfg, getter.get(OutputDirectoryHierarchy.class),
						getter.get(IterationCounter.class)))
				.build());
	}
}
