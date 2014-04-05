/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ReplanningContextImpl.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.controller;

import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class ReplanningContextFactoryImpl implements ReplanningContextFactory {

    @Inject Provider<TravelDisutility> travelDisutility;
    @Inject Provider<TravelTime> travelTime;
    @Inject Provider<TripRouter> tripRouter;
    @Inject ScoringFunctionFactory scoringFunctionFactory;

    @Override
    public ReplanningContext create(final int iteration) {
        return new ReplanningContext() {
            @Override
            public TravelDisutility getTravelDisutility() {
                return travelDisutility.get();
            }

            @Override
            public TravelTime getTravelTime() {
                return travelTime.get();
            }

            @Override
            public TripRouter getTripRouter() {
                return tripRouter.get();
            }

            @Override
            public ScoringFunctionFactory getScoringFunctionFactory() {
                return scoringFunctionFactory;
            }

            @Override
            public int getIteration() {
                return iteration;
            }
        };
    }

}
