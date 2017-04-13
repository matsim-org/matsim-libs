/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.taxibus.algorithm.optimizer.prebooked;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CentroidBasedRequestDeterminatorAndFilter implements RequestDeterminator, RequestFilter {

	final double radius1;
	final double radius2;
	final Coord coord1;
	final Coord coord2;

	/**
	 * 
	 */
	public CentroidBasedRequestDeterminatorAndFilter(Coord coord1, Coord coord2, double radius1, double radius2) {
		this.coord1 = coord1;
		this.coord2 = coord2;
		this.radius1 = radius1;
		this.radius2 = radius2;
	}

	@Override
	public boolean isRequestServable(Request request) {
		TaxibusRequest r = (TaxibusRequest)request;
		Coord fromCoord = r.getFromLink().getCoord();
		Coord toCoord = r.getToLink().getCoord();
		if (((CoordUtils.calcEuclideanDistance(fromCoord, coord1) <= radius1)
				&& (CoordUtils.calcEuclideanDistance(toCoord, coord2) <= radius2))
				|| ((CoordUtils.calcEuclideanDistance(fromCoord, coord2) <= radius2)
						&& (CoordUtils.calcEuclideanDistance(toCoord, coord1) <= radius1)))
			return true;
		else
			return false;
	}

	@Override
	public List<Set<TaxibusRequest>> prefilterRequests(Set<TaxibusRequest> requests) {
		// 0-3: Centroid 1 (by quadrant)
		// 4-7: Centroid 2 (by quadrant)
		List<Set<TaxibusRequest>> filteredRequests = new ArrayList<>();
		for (int i = 0; i <= 7; i++) {
			filteredRequests.add(new HashSet<TaxibusRequest>());
		}
		for (TaxibusRequest r : requests) {
			final Coord fromCoord = r.getFromLink().getCoord();
			int quad;
			double r1_distance = DistanceUtils.calculateDistance(fromCoord, coord1);
			double r2_distance = DistanceUtils.calculateDistance(fromCoord, coord2);
			if (r1_distance <= r2_distance) {
				if (fromCoord.getX() > coord1.getX() && fromCoord.getY() > coord1.getY())
					quad = 0;
				else if (fromCoord.getX() <= coord1.getX() && fromCoord.getY() > coord1.getY())
					quad = 1;
				else if (fromCoord.getX() <= coord1.getX() && fromCoord.getY() < coord1.getY())
					quad = 2;
				else
					quad = 3;
			} else {
				if (fromCoord.getX() > coord2.getX() && fromCoord.getY() > coord2.getY())
					quad = 4;
				else if (fromCoord.getX() <= coord2.getX() && fromCoord.getY() > coord2.getY())
					quad = 5;
				else if (fromCoord.getX() <= coord2.getX() && fromCoord.getY() < coord2.getY())
					quad = 6;
				else
					quad = 7;

			}
			filteredRequests.get(quad).add(r);
		}
		// Logger.getLogger(getClass()).info("Filtered cluster sizes: ");
		// for (int i =0;i<=7;i++){
		// Logger.getLogger(getClass()).info("Quad: "+i+" requests: "+filteredRequests.get(i).size());
		// }
		return filteredRequests;
	}

}
