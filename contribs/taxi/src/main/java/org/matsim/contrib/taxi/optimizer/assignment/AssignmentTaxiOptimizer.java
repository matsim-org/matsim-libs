/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.TreeSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class AssignmentTaxiOptimizer extends DefaultTaxiOptimizer {
	public AssignmentTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
			AssignmentTaxiOptimizerParams params) {
		this(taxiCfg, fleet, scheduler, params,
				new AssignmentRequestInserter(fleet, network, timer, travelTime, travelDisutility, scheduler, params));
	}

	public AssignmentTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			AssignmentTaxiOptimizerParams params, AssignmentRequestInserter requestInserter) {
		super(taxiCfg, fleet, scheduler, params, requestInserter,
				new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), true, true);
	}
}
