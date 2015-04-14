/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LeastCostPathCalculatorModule.java
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

package org.matsim.core.router;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.*;

import javax.inject.Inject;
import javax.inject.Provider;

public class LeastCostPathCalculatorModule extends AbstractModule {

    @Override
    public void install() {
        bind(LeastCostPathCalculatorFactory.class).toProvider(DefaultLeastCostPathCalculatorFactoryProvider.class).in(Singleton.class);
    }

    static class DefaultLeastCostPathCalculatorFactoryProvider implements Provider<LeastCostPathCalculatorFactory> {

        @Inject
        Scenario scenario;

        @Override
        public LeastCostPathCalculatorFactory get() {
            Config config = scenario.getConfig();
            if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
                return new DijkstraFactory();
            } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
                return new AStarLandmarksFactory(
                        scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()), config.global().getNumberOfThreads());
            } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
                return new FastDijkstraFactory();
            } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
                return new FastAStarLandmarksFactory(
                        scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
            } else {
                throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of LeastCostPathCalculatorModule!");
            }
        }

    }

}
