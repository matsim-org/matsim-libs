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

import static playground.boescpa.converters.osm.scheduleCreator.PtRouteFPLAN.BUS;

/**
 * What is it for?
 *
 * @author boescpa
 */
public class PTLRFastAStarLandmarks implements PTLRouter {

    private final static double FACTOR_SAMELINE = 1.0;
    private final static double FACTOR_SAMEMODE = 10.0;
    private final static double FACTOR_PTLINK = 100.0;
    private final static double FACTOR_LINKTYPE = 1000.0;
    private final static double FACTOR_NOMATCH = 1000000.0;

    private final LeastCostPathCalculator pathCalculator;
    private String currentMode;
    private String currentLine;

    public PTLRFastAStarLandmarks(Network network) {
        LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
        this.pathCalculator = factory.createPathCalculator(network, this, this);
    }

    @Override
    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode, String routeId) {
        this.currentMode = mode;
        this.currentLine = deriveLineFromRouteId(routeId);
        return pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return this.getLinkMinimumTravelDisutility(link);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        double travelCost = link.getLength()/link.getFreespeed();

        // todo-boescpa Implement!!!

        /*
        if (currentMode.equals(BUS)) {
            if (link.getAllowedModes().contains("street")) {

            }
        }

        if (link.getAllowedModes().contains(currentMode)) {
            if (link.getAllowedModes().contains(currentLine)) {
                return travelCost * FACTOR_SAMELINE;
            } else {
                return travelCost * FACTOR_SAMEMODE;
            }
        } else if (currentMode.equals(BUS)) {
            if (link.getAllowedModes().contains("street")) {
                return travelCost * FACTOR_LINKTYPE;
            }
        } else {
            return travelCost * FACTOR_NOMATCH;
        }*/
        return travelCost;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }

    private String deriveLineFromRouteId(String routeId) {
        return null;
    }
}
