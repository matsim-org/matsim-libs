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

package org.matsim.vsp.edvrp.edrt.run;

import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.DrtMainModeIdentifier;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EDrtModule extends AbstractModule {

	@Inject
	private DrtConfigGroup drtCfg;

	@Override
	public void install() {
		install(new DrtModeModule(drtCfg));
		installQSimModule(new EDrtModeQSimModule(drtCfg));
		install(new DrtModeAnalysisModule(drtCfg));

		bind(MainModeIdentifier.class).to(DrtMainModeIdentifier.class).asEagerSingleton();
		install(DvrpTravelDisutilityModule.createWithTimeAsTravelDisutility(Drt.class));
	}
}
