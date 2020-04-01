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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class SequentialMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem {
	private final SingleVehicleInsertionProblem insertionProblem;
	private final InsertionGenerator insertionGenerator = new InsertionGenerator();

	public SequentialMultiVehicleInsertionProblem(Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, DrtConfigGroup drtCfg, MobsimTimer timer,
			InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this(new SequentialPathDataProvider(network, travelTime, travelDisutility, drtCfg), drtCfg, timer,
				penaltyCalculator);
	}

	public SequentialMultiVehicleInsertionProblem(PathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer, InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.insertionProblem = new SingleVehicleInsertionProblem(pathDataProvider,
				new InsertionCostCalculator(drtCfg, timer, penaltyCalculator));
	}

	@Override
	public Optional<BestInsertion> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		return vEntries.stream()
				.map(v -> insertionProblem.findBestInsertion(drtRequest, v,
						insertionGenerator.generateInsertions(drtRequest, v)))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.min(Comparator.comparing(i -> i.cost));
	}
}
