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

package playground.polettif.boescpa.converters.osm.ptMapping;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN.BUS;
import static playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN.TRAM;

/**
 * Based on the line, mode, and link type, the traveling on links is assigned different costs.
 * The better the match, the lower the costs.
 *
 * @author boescpa
 */
public class PTLRFastAStarLandmarksWeighting implements PTLRouter {

	private final static double FACTOR_SAMELINE = 1.0;
    private final static double FACTOR_SAMEMODE = 10.0;
    private final static double FACTOR_PTLINK = 10.0;
    private final static double FACTOR_LINKTYPE = 100.0;
    private final static double FACTOR_NOMATCH = 100000.0;

    private final Dijkstra pathCalculator;
    private final Map<Tuple<Node, Node>, LeastCostPathCalculator.Path> paths = new HashMap<>();
    private String currentLine;

	private String currentMode;
	private void setMode(String mode) {
		switch (mode) {
			case BUS: {
				this.currentMode = BUS;
				this.pathCalculator.setModeRestriction(Collections.singleton("car"));
				break;
			}
			case TRAM: {
				this.currentMode = TRAM;
				this.pathCalculator.setModeRestriction(Collections.singleton("tram"));
				break;
			}
			default: {
				this.currentMode = null;
				this.pathCalculator.setModeRestriction(null);
			}
		}
	}

    public PTLRFastAStarLandmarksWeighting(Network network) {
        LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(network, this);
        this.pathCalculator = (Dijkstra) factory.createPathCalculator(network, this, this);
		// Suppress "no route found" statements...
		Logger.getLogger( org.matsim.core.router.Dijkstra.class ).setLevel( Level.ERROR );
    }

    @Override
    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, String mode, String routeId) {
        this.setMode(mode);
        this.currentLine = deriveLineFromRouteId(routeId);
        if (fromNode != null && toNode != null) {
            Tuple<Node, Node> nodes = new Tuple<>(fromNode, toNode);
            if (!paths.containsKey(nodes)) {
                paths.put(nodes, pathCalculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null));
            }
            return paths.get(nodes);
        } else {
            return null;
        }
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return this.getLinkMinimumTravelDisutility(link);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        double travelCost = link.getLength()/link.getFreespeed();

        // Trams: They drive on tram-railways, preferably on those with their line number.
        if (this.currentMode != null && this.currentMode.equals(TRAM)) {
            if (this.currentLine != null && link.getAllowedModes().contains(this.currentLine)) {
                return travelCost * FACTOR_SAMELINE;
            } else {
                return travelCost * FACTOR_SAMEMODE;
            }
        }

        // Busses: They drive on streets ("car"), preferably on those for "pt", preferably on those with their line number.
        if (this.currentMode != null && this.currentMode.equals(BUS)) {
            if (link.getAllowedModes().contains("pt")) {
                if (this.currentLine != null && link.getAllowedModes().contains(this.currentLine)) {
                    return travelCost * FACTOR_SAMELINE;
                }
                return travelCost * FACTOR_PTLINK;
            }
            return travelCost * FACTOR_LINKTYPE;
        }

        // If the link has no match, it is terribly expensive to travel on it...
        return travelCost * FACTOR_NOMATCH;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }

    private String deriveLineFromRouteId(String routeId) {
        if (routeId != null && routeId.length() > 3) {
            return routeId.substring(0, 4);
        } else {
            return null;
        }
    }
}
