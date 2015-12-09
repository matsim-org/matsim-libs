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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;

class ReplanningContextImpl implements ReplanningContext {

    private int iteration;
    private Config config;
    private Map<String, TravelDisutilityFactory> travelDisutility;
    private Map<String, TravelTime> travelTime;
    private Provider<ScoringFunctionFactory> scoringFunctionFactory;

    @Inject
    ReplanningContextImpl(ControlerI controler, Config config, Map<String, TravelDisutilityFactory> travelDisutility, Map<String, TravelTime> travelTime, Provider<ScoringFunctionFactory> scoringFunctionFactory) {
        this.iteration = controler.getIterationNumber();
        this.config = config;
        this.travelDisutility = travelDisutility;
        this.travelTime = travelTime;
        this.scoringFunctionFactory = scoringFunctionFactory;
    }

    @Override
    public TravelDisutility getTravelDisutility() {
        return travelDisutility.get(TransportMode.car).createTravelDisutility(travelTime.get(TransportMode.car), config.planCalcScore());
    }

    @Override
    public TravelTime getTravelTime() {
        return travelTime.get(TransportMode.car);
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
