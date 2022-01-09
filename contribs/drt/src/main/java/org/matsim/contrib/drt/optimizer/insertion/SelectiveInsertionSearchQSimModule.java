/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.InsertionCostCalculatorFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.TypeLiteral;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SelectiveInsertionSearchQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public SelectiveInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		bindModal(new TypeLiteral<DrtInsertionSearch<PathData>>() {
		}).toProvider(modalProvider(getter -> {
			var insertionCostCalculatorFactory = getter.getModal(InsertionCostCalculatorFactory.class);
			var provider = SelectiveInsertionProvider.create(drtCfg, insertionCostCalculatorFactory,
					getter.getModal(DvrpTravelTimeMatrix.class), getter.getModal(TravelTime.class),
					getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool());
			// Use 0 as the cost for the selected insertion:
			// - In the selective strategy, there is at most 1 insertion pre-selected. So no need to compute as there is
			//   no other insertion to compare with.
			// - We assume that the travel times obtained from DvrpTravelTimeMatrix are reasonably well estimated(*),
			//   so we do not want to check for time window violations
			//  Re (*) currently, free-speed travel times are quite accurate. We still need to adjust them to different times of day.
			InsertionCostCalculator zeroCostInsertionCostCalculator = (drtRequest, insertion, detourTimeInfo) -> 0;
			return new DefaultDrtInsertionSearch(provider, getter.getModal(DetourPathCalculator.class),
					zeroCostInsertionCostCalculator, drtCfg.getStopDuration());
		})).asEagerSingleton();

		addModalComponent(SingleInsertionDetourPathCalculator.class,
				new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
					@Override
					public SingleInsertionDetourPathCalculator get() {
						var travelTime = getModalInstance(TravelTime.class);
						Network network = getModalInstance(Network.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						return new SingleInsertionDetourPathCalculator(network, travelTime, travelDisutility, drtCfg);
					}
				});
		bindModal(DetourPathCalculator.class).to(modalKey(SingleInsertionDetourPathCalculator.class));
	}
}
