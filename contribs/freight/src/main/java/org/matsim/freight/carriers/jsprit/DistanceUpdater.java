/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.algorithm.state.StateUpdater;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

/**
	 * Given class for working with the distance constraint
	 *
	 */
	/*package-private*/ class DistanceUpdater implements StateUpdater, ActivityVisitor {

		private final StateManager stateManager;

		private final StateId distanceStateId;

		private VehicleRoute vehicleRoute;

		private double distance = 0.;

		private TourActivity prevAct;

		private final NetworkBasedTransportCosts netBasedCosts;

		public DistanceUpdater(StateId distanceStateId, StateManager stateManager,
				NetworkBasedTransportCosts netBasedCosts) {
			this.stateManager = stateManager;
			this.distanceStateId = distanceStateId;
			this.netBasedCosts = netBasedCosts;
		}

		@Override
		public void begin(VehicleRoute vehicleRoute) {
			distance = 0.;
			prevAct = vehicleRoute.getStart();
			this.vehicleRoute = vehicleRoute;
		}

		@Override
		public void visit(TourActivity tourActivity) {
			distance += getDistance(prevAct, tourActivity);
			prevAct = tourActivity;
		}

		@Override
		public void finish() {
			distance += getDistance(prevAct, vehicleRoute.getEnd());
			stateManager.putRouteState(vehicleRoute, distanceStateId, distance);
		}

		double getDistance(TourActivity from, TourActivity to) {
			double distance = netBasedCosts.getDistance(from.getLocation(), to.getLocation(), 0, null);
			if (!(distance >= 0.))
				throw new AssertionError("Distance must not be negative! From, to" + from + ", " + to + " distance " + distance);
			return distance;
		}
	}


