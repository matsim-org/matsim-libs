/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.osm.ptRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.Vehicle;

/**
 * What is it for?
 *
 * @author boescpa
 */
public class PTLRFastAStarLandmarks implements PTLRouter {

    private final LeastCostPathCalculator pathCalculator;

    public PTLRFastAStarLandmarks(Network network) {
        LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
        this.pathCalculator = factory.createPathCalculator(network, this, this);
    }

    @Override
    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode) {
        return pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return this.getLinkMinimumTravelDisutility(link);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        // TODO-boescpa implement cost-calculator for each link. Cost must be non-negative.
        return 0;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }
}
