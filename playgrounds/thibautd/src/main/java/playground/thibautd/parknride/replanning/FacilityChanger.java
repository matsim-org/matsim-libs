/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityChanger.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.parknride.ParkAndRideConstants;
import playground.thibautd.parknride.ParkAndRideFacilities;
import playground.thibautd.parknride.ParkAndRideFacility;
import playground.thibautd.parknride.scoring.ParkingPenalty;
import playground.thibautd.parknride.scoring.ParkingPenaltyFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class FacilityChanger {
	private final double searchRadius;
	private final QuadTree<ParkAndRideFacility> facilitiesQT;
	private final ParkAndRideFacilities facilities;
	private final ParkingPenaltyFactory penalty;
	private final Random random;
	private final TripRouter router;
	private final double priceOfDistance;

	public FacilityChanger(
			final Random random,
			final TripRouter router,
			final ParkingPenaltyFactory penalty,
			final ParkAndRideFacilities facilities,
			final double searchRadius,
			final double priceOfDistance) {
		this.router = router;
		this.penalty = penalty;
		this.searchRadius = searchRadius;
		this.facilities = facilities;
		this.facilitiesQT = createQuadTree( facilities );
		this.random = random;
		this.priceOfDistance = priceOfDistance;
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

	public void changePnrFacilityAndRouteSubtour(
			final Plan plan,
			final List<PlanElement> subtour) {
		Activity pnrFirst = (Activity) subtour.get(2);
		Activity pnrLast = (Activity) subtour.get( subtour.size() - 3 );

		if ( !pnrFirst.getType().equals( ParkAndRideConstants.PARKING_ACT ) ) {
			throw new RuntimeException( "What!? "+pnrFirst.getType()+" in "+subtour );
		}
		if ( !pnrLast.getType().equals( ParkAndRideConstants.PARKING_ACT ) ) {
			throw new RuntimeException( "What!? "+pnrLast.getType()+" in "+subtour );
		}

		ParkAndRideFacility fac = facilities.getFacilities().get( pnrFirst.getFacilityId() );
		Collection<ParkAndRideFacility> neighbors = getNeighbors( fac , searchRadius );

		ParkAndRideFacility newFac =
			chooseFac(
					random.nextDouble(),
					neighbors,
					penalty.createPenalty( plan ),
					// the subtour is a "structure", so that trips are one leg
					((Activity) subtour.get( 0 )).getCoord(),
					((Activity) subtour.get( 4 )).getCoord(),
					((Activity) subtour.get( subtour.size() - 5 )).getCoord(),
					ParkAndRideChooseModeForSubtour.getEndTime( pnrFirst , plan.getPlanElements()),
					ParkAndRideChooseModeForSubtour.getEndTime( pnrLast, plan.getPlanElements() ),
					priceOfDistance);

		updateFacility( pnrFirst , newFac );
		updateFacility( pnrLast , newFac );

		rerouteSubtour( plan , subtour );
	}

	private Collection<ParkAndRideFacility> getNeighbors(
			final ParkAndRideFacility fac,
			final double currentRadius) {
		Collection<ParkAndRideFacility> neighbors =
				facilitiesQT.getDisk(
						fac.getCoord().getX(),
						fac.getCoord().getY(),
						currentRadius);
		neighbors.remove( fac );
		// TODO: max recursion depth / maximum search radius?
		return neighbors.size() > 1 ? neighbors : getNeighbors( fac , currentRadius * 2 );
	}

	private static void updateFacility(
			final Activity act,
			final ParkAndRideFacility newFac) {
		((ActivityImpl) act).setFacilityId( newFac.getId() );
		((ActivityImpl) act).setLinkId( newFac.getLinkId() );
		((ActivityImpl) act).setCoord( newFac.getCoord() );
	}

	private static ParkAndRideFacility chooseFac(
			final double randomChoice,
			final Collection<ParkAndRideFacility> neighbors,
			final ParkingPenalty penalty,
			final Coord origin,
			final Coord accessDestination,
			final Coord egressOrigin,
			final double parkTime,
			final double unparkTime,
			final double priceOfDistance) {
		ParkAndRideFacility[] fs = new ParkAndRideFacility[neighbors.size()];
		double[] ps = new double[fs.length];
		double sum = 0;

		int i=0;
		for (ParkAndRideFacility f : neighbors) {
			fs[ i ] = f;

			penalty.reset();
			penalty.park( parkTime , f.getCoord() );
			penalty.unPark( unparkTime );
			penalty.finish();

			double dist = calcDistance( origin , f.getCoord() , accessDestination , egressOrigin );

			// "logit-like" choice
			// The variance will depend on the scale of the penalty...
			sum += Math.exp( penalty.getPenalty() - dist * priceOfDistance );
			ps[ i ] = sum;
			i++;
		}

		double choice = randomChoice * sum;
		for (i = 0; i < ps.length; i++) {
			if (choice <= ps[ i ]) {
				return fs[ i ];
			}
		}

		throw new RuntimeException( "unexpected. choice="+choice+", sum="+sum );
	}

	private static double calcDistance(
			final Coord origin,
			final Coord pnr,
			final Coord accessDest,
			final Coord egressOr) {
		return CoordUtils.calcEuclideanDistance( origin , pnr ) * 2
			+ CoordUtils.calcEuclideanDistance( pnr , accessDest )
			+ CoordUtils.calcEuclideanDistance( pnr , egressOr );
	}

	private void rerouteSubtour(
			final Plan plan,
			final List<PlanElement> subtour) {
		Iterator<PlanElement> iter = subtour.iterator();
		List<PlanElement> planElements = plan.getPlanElements();
		Person person = plan.getPerson();

		if ( iter.hasNext() ) {
			Activity origin = (Activity) iter.next();

			while (iter.hasNext()) {
				PlanElement pe = iter.next();
				if (pe instanceof Activity) {
					// this can happen due to skipping sub-subtours.
					// just do one step forward.
					origin = (Activity) pe;
					pe = iter.next();
				}

				String mode = getMode( pe ); // skip leg
				Activity destination = (Activity) iter.next();
				List<? extends PlanElement> trip =
					router.calcRoute(
							mode,
							new ActivityWrapperFacility( origin ),
							new ActivityWrapperFacility( destination ),
							ParkAndRideChooseModeForSubtour.getEndTime( origin , planElements ),
							person);
				TripRouter.insertTrip(
						planElements,
						origin,
						trip,
						destination);
				origin = destination;
			}
		}
	}

	private static String getMode(final PlanElement leg) {
		String mode = ((Leg) leg).getMode();

		if ( !Arrays.asList( TransportMode.car , TransportMode.pt ).contains( mode ) ) {
			throw new RuntimeException( "unexpected mode "+mode );
		}

		return mode;
	}
}

