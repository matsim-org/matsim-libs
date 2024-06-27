/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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


package org.matsim.contrib.drt.optimizer.insertion.repeatedselective;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.selective.SingleInsertionDetourPathCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.zone.skims.AdaptiveTravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author steffenaxer
 */
public class RepeatedSelectiveInsertionSearchQSimModule extends AbstractDvrpModeQSimModule {
    private static final double SPEED_FACTOR = 1.;
    
    private final DrtConfigGroup drtCfg;

    public RepeatedSelectiveInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
        super(drtCfg.getMode());
        this.drtCfg = drtCfg;
    }

    @Override
    protected void configureQSim() {
		bindModal(DetourTimeEstimator.class).toProvider(modalProvider(getter -> {
			var detourTimeEstimatorWithUpdatedTravelTimes = DetourTimeEstimatorWithAdaptiveTravelTimes.create(
					SPEED_FACTOR, getter.getModal(AdaptiveTravelTimeMatrix.class), getter.getModal(TravelTime.class));
			return detourTimeEstimatorWithUpdatedTravelTimes;
		}));
    	
        addModalComponent(RepeatedSelectiveInsertionSearch.class, modalProvider(getter -> {
			RepeatedSelectiveInsertionProvider provider = RepeatedSelectiveInsertionProvider.create(
					getter.getModal(InsertionCostCalculator.class),
					getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool(),
					getter.getModal(StopTimeCalculator.class), getter.getModal(DetourTimeEstimator.class));
            var insertionCostCalculator = getter.getModal(InsertionCostCalculator.class);
            return new RepeatedSelectiveInsertionSearch(provider,
                    getter.getModal(SingleInsertionDetourPathCalculator.class),
                    insertionCostCalculator, drtCfg, getter.get(MatsimServices.class),
                    getter.getModal(StopTimeCalculator.class), getter.getModal(TravelTimeMatrix.class),
                    getter.getModal(AdaptiveTravelTimeMatrix.class));
        }));
        bindModal(DrtInsertionSearch.class).to(modalKey(RepeatedSelectiveInsertionSearch.class));

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
    }
}
