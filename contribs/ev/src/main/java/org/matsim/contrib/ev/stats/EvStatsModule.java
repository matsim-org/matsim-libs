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
import org.matsim.core.controler.AbstractModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EvStatsModule extends AbstractModule {
	private final EvConfigGroup evCfg;

	public EvStatsModule(EvConfigGroup evCfg) {
		this.evCfg = evCfg;
	}

	@Override
	public void install() {
		if (evCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(SocHistogramTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(IndividualSocTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(ChargerOccupancyTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(ChargerOccupancyXYDataProvider.class);
			addMobsimListenerBinding().toProvider(VehicleTypeAggregatedSocTimeProfileCollectorProvider.class);
			// add more time profiles if necessary
		}
		addControlerListenerBinding().to(EvControlerListener.class).asEagerSingleton();
		bind(ChargerPowerCollector.class).asEagerSingleton();
	}
}
