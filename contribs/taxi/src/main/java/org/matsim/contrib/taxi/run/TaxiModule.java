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

import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityModule;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public final class TaxiModule extends AbstractModule {

	@Inject
	private TaxiConfigGroup taxiCfg;

	@Override
	public void install() {
		install(new TaxiModeModule(taxiCfg));
		installQSimModule(new TaxiModeQSimModule(taxiCfg));

		install(DvrpTravelDisutilityModule.createWithTimeAsTravelDisutility(Taxi.class));
	}
}
