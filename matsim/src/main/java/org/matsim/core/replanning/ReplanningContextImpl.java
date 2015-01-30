/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ReplanningContextImpl.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.core.replanning;

import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

class ReplanningContextImpl implements ReplanningContext {

    private int iteration;
    private Provider<TravelDisutility> travelDisutility;
    private Provider<TravelTime> travelTime;
    private Provider<TripRouter> tripRouter;
    private Provider<ScoringFunctionFactory> scoringFunctionFactory;

    @Inject
    ReplanningContextImpl(@Named("iteration") int iteration, Provider<TravelDisutility> travelDisutility, Provider<TravelTime> travelTime, Provider<TripRouter> tripRouter, Provider<ScoringFunctionFactory> scoringFunctionFactory) {
        this.iteration = iteration;
        this.travelDisutility = travelDisutility;
        this.travelTime = travelTime;
        this.tripRouter = tripRouter;
        this.scoringFunctionFactory = scoringFunctionFactory;
    }

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
        return scoringFunctionFactory.get();
    }

    @Override
    public int getIteration() {
        return iteration;
    }
}
