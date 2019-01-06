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

package org.matsim.contrib.drt.run;

import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.MultiModeDrtMainModeIdentifier;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;

import com.google.inject.Inject;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class MultiModeDrtModule extends AbstractModule {

	@Inject
	private MultiModeDrtConfigGroup multiModeDrtCfg;

	@Override
	public void install() {
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			install(new DrtModeModule(drtCfg));
			installQSimModule(new DrtModeQSimModule(drtCfg));
			install(new DrtModeAnalysisModule(drtCfg));
		}

		bind(MainModeIdentifier.class).toInstance(new MultiModeDrtMainModeIdentifier(multiModeDrtCfg));
		install(DvrpTravelDisutilityModule.createWithTimeAsTravelDisutility(Drt.class));
	}
}
