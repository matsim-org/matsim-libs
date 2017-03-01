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

package playground.michalm.taxi.optimizer.rules;

import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.contrib.zone.ZonalSystem;

public class RuleBasedTaxiOptimizerWithRelocation extends RuleBasedTaxiOptimizer {
	// plugging this optimizer into Matsim requires some coding
	// we are working on making it easier
	// for the time being please have a look at ETaxiQSimProvider, where we plug non-standard taxi algos
	public RuleBasedTaxiOptimizerWithRelocation(TaxiOptimizerContext optimContext, RuleBasedTaxiOptimizerParams params,
			ZonalSystem zonalSystem) {
		super(optimContext, params, zonalSystem);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		// re-schedule
		super.scheduleUnplannedRequests();

		// relocate empty vehicles
		relocateEmptyVehicles();
	}

	private void relocateEmptyVehicles() {
		// if oversupply then estimate (current & near-future) demand and supply per zone
		// solve transportation problem

		// comment: idleTaxiRegistry is accessible from the upper class
		// I would try to use the same zonal system for idleTaxiRegistry as is used for relocation
	}
}
