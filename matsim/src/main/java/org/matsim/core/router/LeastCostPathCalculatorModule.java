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

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.*;

public class LeastCostPathCalculatorModule extends AbstractModule {

    @Override
    public void install() {
	    // yy The code below will install _one_ LeastCostPathCalculator, which will be Dijkstra or Landmarks or something.  It will be the
	    // same Landmarks instance for all modes ... although one could do better by doing the preprocessing separately for the different modes.
	    // kai/mm, jan'17

        Config config = getConfig();
        if (config.controller().getRoutingAlgorithmType().equals(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
            bind(LeastCostPathCalculatorFactory.class).to(DijkstraFactory.class);
        } else if (config.controller().getRoutingAlgorithmType().equals(ControllerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
            bind(LeastCostPathCalculatorFactory.class).to(AStarLandmarksFactory.class);
        } else if (config.controller().getRoutingAlgorithmType().equals(ControllerConfigGroup.RoutingAlgorithmType.SpeedyALT)) {
            bind(LeastCostPathCalculatorFactory.class).to(SpeedyALTFactory.class);
        }
    }

}
