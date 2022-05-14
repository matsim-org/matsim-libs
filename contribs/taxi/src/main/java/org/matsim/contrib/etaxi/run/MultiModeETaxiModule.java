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

import org.matsim.contrib.drt.run.DrtModeModule;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.etaxi.optimizer.ETaxiModeOptimizerQSimModule;
import org.matsim.contrib.taxi.analysis.TaxiModeAnalysisModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiAsDrtConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModeModule;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public final class MultiModeETaxiModule extends AbstractModule {

	@Inject
	private MultiModeTaxiConfigGroup multiModeTaxiCfg;

	@Override
	public void install() {
		for (TaxiConfigGroup taxiCfg : multiModeTaxiCfg.getModalElements()) {
			var drtCfg = TaxiAsDrtConfigGroup.convertTaxiToDrtCfg(taxiCfg);
			install(new DrtModeModule(drtCfg));
			install(new TaxiModeModule(taxiCfg));
			installQSimModule(new DrtModeQSimModule(drtCfg, new ETaxiModeOptimizerQSimModule(taxiCfg)));
			install(new TaxiModeAnalysisModule(taxiCfg));
		}
	}
}
