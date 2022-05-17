/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ExtensiveInsertionSearchQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public ExtensiveInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		bindModal(DrtInsertionSearch.class).toProvider(modalProvider(getter -> {
			var insertionCostCalculator = getter.getModal(InsertionCostCalculator.class);
			var provider = ExtensiveInsertionProvider.create(drtCfg, insertionCostCalculator,
					getter.getModal(TravelTimeMatrix.class), getter.getModal(TravelTime.class),
					getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool());
			return new ExtensiveInsertionSearch(provider, getter.getModal(MultiInsertionDetourPathCalculator.class),
					insertionCostCalculator, drtCfg.getStopDuration());
		})).asEagerSingleton();

		addModalComponent(MultiInsertionDetourPathCalculator.class,
				new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
					@Override
					public MultiInsertionDetourPathCalculator get() {
						var travelTime = getModalInstance(TravelTime.class);
						Network network = getModalInstance(Network.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						return new MultiInsertionDetourPathCalculator(network, travelTime, travelDisutility, drtCfg);
					}
				});
	}
}
