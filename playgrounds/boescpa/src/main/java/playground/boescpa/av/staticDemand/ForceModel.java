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

package playground.boescpa.av.staticDemand;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.analysis.trips.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class ForceModel {

	public Force resultingForce (Coord origin, List<Trip> openRequests, Collection<AutonomousVehicle> freeAVs) {
		// get all acting forces:
		List<Force> actingForces = new ArrayList<>();
		if (openRequests != null) {
			for (Trip request : openRequests) {
				actingForces.add(getForce(origin, CoordUtils.createCoord(request.startXCoord, request.startYCoord), 1));
			}
		}
		if (freeAVs != null) {
			for (AutonomousVehicle freeAV : freeAVs) {
				actingForces.add(getForce(origin, freeAV.getMyPosition(), -1));
			}
		}
		// sum acting forces:
		double xPart = 0;
		double yPart = 0;
		for (Force actingForce : actingForces) {
			xPart += actingForce.xPart;
			yPart += actingForce.yPart;
		}
		// return resulting force:
		return new Force(xPart, yPart);
	}

	private Force getForce(Coord A, Coord B, int weightProduct) {
		double distance = CoordUtils.calcEuclideanDistance(A, B);
		double xPart = B.getX()-A.getX();
		double yPart = B.getY()-A.getY();
		double weight = 0;
		if (distance != 0) {
			weight = weightProduct/(distance*distance*distance);
		}
		return new Force(weight*xPart, weight*yPart);
	}

	public class Force {
		public final double xPart;
		public final double yPart;
		public int identifier;

		public Force(double xPart, double yPart) {
			if (Double.isNaN(xPart) || Double.isNaN(yPart)) {
				throw new IllegalArgumentException("NaN-parts for new force!");
			}
			this.xPart = xPart;
			this.yPart = yPart;
		}

		public double getStrength() {
			return Math.sqrt((xPart*xPart) + (yPart*yPart));
		}
	}

}
