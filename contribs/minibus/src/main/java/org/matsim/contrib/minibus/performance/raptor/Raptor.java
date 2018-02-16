/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
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
 * *********************************************************************** */

package org.matsim.contrib.minibus.performance.raptor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.InitialNode;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Wrapper for {@linkplain RaptorWalker}.
 *
 * @author aneumann
 */
public class Raptor extends AbstractTransitRouter implements TransitRouter {

	private final TransitRouterQuadTree transitRouterQuadTree;

	private final RaptorWalker raptorWalker;
	private final TransitRouterConfig config;
	private RaptorDisutility raptorDisutility ;

	// MAGIC NUMBERS BELOW THIS LINE
	int maxTransfers = 10;
	int graceRuns = 1;
	// END MAGIC NUMBERS

	public Raptor(TransitRouterConfig transitRouterConfig, TransitSchedule transitSchedule, TransitTravelDisutility raptorDisutility) {
		super (transitRouterConfig, raptorDisutility);
		this.config = transitRouterConfig;
		// casting to raptorDisutility is necessary here. At the moment, I dont know if there is a better way. Amit Oct'17
		this.transitRouterQuadTree = new TransitRouterQuadTree( (RaptorDisutility) raptorDisutility);
		this.transitRouterQuadTree.initializeFromSchedule(transitSchedule, config.getBeelineWalkConnectionDistance());
		this.raptorDisutility = (RaptorDisutility) raptorDisutility;
		this.raptorWalker = new RaptorWalker(this.transitRouterQuadTree.getSearchData(), raptorDisutility, this.maxTransfers, this.graceRuns);
	}

	public Raptor(TransitRouterConfig transitRouterConfig, TransitSchedule transitSchedule) {
		// do something about these two magic numbers for raptor disutility. Amit Oct'17
		this(transitRouterConfig, transitSchedule, new RaptorDisutility(transitRouterConfig, 0., 0.));
	}

	private Map<TransitStopFacility, InitialNode> locateWrappedNearestTransitStops(Person person, Coord coord, double departureTime) {
		Collection<TransitStopFacility> nearestTransitStops = this.transitRouterQuadTree.getNearestTransitStopFacilities(coord, this.config.getSearchRadius());
		if (nearestTransitStops.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitStopFacility nearestTransitStop = this.transitRouterQuadTree.getNearestTransitStopFacility(coord);
			double distance = CoordUtils.calcEuclideanDistance(coord, nearestTransitStop.getCoord());
			nearestTransitStops = this.transitRouterQuadTree.getNearestTransitStopFacilities(coord, distance + this.config.getExtensionRadius());
		}
		Map<TransitStopFacility, InitialNode> wrappedNearestTransitStops2AccessCost = new LinkedHashMap<>();
		for (TransitStopFacility node : nearestTransitStops) {
			Coord toCoord = node.getCoord();
			double initialTime = getWalkTime(null, coord, toCoord);
			double initialCost = getWalkDisutility(coord, toCoord);
			wrappedNearestTransitStops2AccessCost.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		return wrappedNearestTransitStops2AccessCost;
	}

	private double getWalkDisutility(Coord coord, Coord toCoord) {
		return this.raptorDisutility.getWalkTravelDisutility(null, coord, toCoord);
	}

	@Override
	public List<Leg> calcRoute(final Facility<?> fromFacility, final Facility<?> toFacility, final double departureTime, final Person person) {
		// find possible start stops
		Map<TransitStopFacility, InitialNode> fromStops = this.locateWrappedNearestTransitStops(person, fromFacility.getCoord(), departureTime);
		// find possible end stops
		Map<TransitStopFacility, InitialNode> toStops = this.locateWrappedNearestTransitStops(person, toFacility.getCoord(), departureTime);

		// find routes between start and end stops
		TransitPassengerRoute p = this.raptorWalker.calcLeastCostPath(fromStops, toStops);

		if (p == null) {
			// return null;
			// returning at least walk legs if no PT route is found. Amit Aug'17
			return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
		}

		double directWalkCost = getWalkDisutility(fromFacility.getCoord(), toFacility.getCoord());
		double pathCost = p.getTravelCost();

		if (directWalkCost * getConfig().getDirectWalkFactor() < pathCost) {
			return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
		}
		return convertPassengerRouteToLegList(departureTime, p, fromFacility.getCoord(), toFacility.getCoord(), person);
	}

}
