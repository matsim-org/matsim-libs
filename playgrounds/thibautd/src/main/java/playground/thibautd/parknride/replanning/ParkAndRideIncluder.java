/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideIncluder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.replanning;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import playground.thibautd.parknride.ParkAndRideConstants;
import playground.thibautd.parknride.ParkAndRideFacilities;
import playground.thibautd.parknride.ParkAndRideFacility;

import java.util.ArrayList;
import java.util.List;


/**
 * @author thibautd
 */
public class ParkAndRideIncluder {
	private final QuadTree<ParkAndRideFacility> quadTree;
	private final TripRouter tripRouter;

	public ParkAndRideIncluder(
			final ParkAndRideFacilities facilities,
			final TripRouter router) {
		quadTree = createQuadTree( facilities );
		tripRouter = router;
	}

	public boolean routeAndIncludePnrTrips(
			final Activity accessOriginActivity,
			final Activity accessDestinationActivity,
			final Activity egressOriginActivity,
			final Activity egressDestinationActivity,
			final Plan plan) {
		Coord anchor = accessOriginActivity.getCoord();
		Coord access = accessDestinationActivity.getCoord();
		Coord egress = egressOriginActivity.getCoord();

		// we take the facility which is the closest to the centroid
		ParkAndRideFacility fac = quadTree.getClosest(
				(anchor.getX() + access.getX() + egress.getX()) / 3d,
				(anchor.getY() + access.getY() + egress.getY()) / 3d);

		routeAndIncludePnr(
				plan,
				accessOriginActivity,
				accessDestinationActivity,
				fac,
				TransportMode.car,
				TransportMode.pt);

		routeAndIncludePnr(
				plan,
				egressOriginActivity,
				egressDestinationActivity,
				fac,
				TransportMode.pt,
				TransportMode.car);

		return true;
	}

	private void routeAndIncludePnr(
			final Plan plan,
			final Activity origin,
			final Activity destination,
			final ParkAndRideFacility fac,
			final String firstMode,
			final String lastMode) {
		List<PlanElement> trip = new ArrayList<PlanElement>();
		ActivityImpl change =
			new ActivityImpl(
					ParkAndRideConstants.PARKING_ACT,
					fac.getCoord(),
					fac.getLinkId());
		change.setFacilityId( fac.getId() );
		change.setMaximumDuration( 0 );

		double depTime = origin.getEndTime();
		trip.addAll( tripRouter.calcRoute(
					firstMode,
					new ActivityWrapperFacility( origin ),
					fac,
					depTime,
					plan.getPerson()) );

		for (PlanElement pe : trip) {
			if (pe instanceof Leg) depTime += ((Leg) pe).getTravelTime();
		}

		trip.add( change );
		trip.addAll( tripRouter.calcRoute(
					lastMode,
					fac,
					new ActivityWrapperFacility( destination ),
					depTime,
					plan.getPerson()) );

		TripRouter.insertTrip(
				plan,
				origin,
				trip,
				destination);
	}

	private static QuadTree<ParkAndRideFacility> createQuadTree(
			final ParkAndRideFacilities facilities) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (ParkAndRideFacility f : facilities.getFacilities().values()) {
			double x = f.getCoord().getX();
			double y = f.getCoord().getY();
			minX = x < minX ? x : minX;
			minY = y < minY ? y : minY;
			maxX = x > maxX ? x : maxX;
			maxY = y > maxY ? y : maxY;
		}

		QuadTree<ParkAndRideFacility> qt =
			new QuadTree<ParkAndRideFacility>(
					minX, minY, maxX, maxY);

		for (ParkAndRideFacility f : facilities.getFacilities().values()) {
			qt.put( f.getCoord().getX() , f.getCoord().getY() , f );
		}

		return qt;
	}
}

