/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingInsertionAlgorithm.java
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
package playground.thibautd.hitchiking.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.hitchiking.HitchHikingConstants;

/**
 * Parses a plan to get pt and car trips, and chooses one at random to transform
 * it to hitch hiking.
 *
 * @author thibautd
 */
public class HitchHikingInsertionAlgorithm implements PlanAlgorithm {
	private final TripRouter router;
	private final Random random;

	public HitchHikingInsertionAlgorithm(
			final Random random,
			final TripRouter router) {
		this.random = random;
		this.router = router;
	}

	@Override
	public void run(final Plan plan) {
		final Person person = plan.getPerson();
		final boolean canBeDriver = person instanceof PersonImpl ? 
			!"never".equals( ((PersonImpl) person).getCarAvail() ):
			true;

		Iterator<PlanElement> structure = router.tripsToLegs( plan ).iterator();
		List<Od> carOds = new ArrayList<Od>();
		List<Od> ptOds = new ArrayList<Od>();

		Activity origin = (Activity) structure.next();
		while (structure.hasNext()) {
			Leg trip = (Leg) structure.next();
			Activity destination = (Activity) structure.next();

			if ( canBeDriver && TransportMode.car.equals( trip.getMode() ) ) {
				carOds.add( new Od( origin , destination ) );
			}
			else if ( TransportMode.pt.equals( trip.getMode() ) ) {
				ptOds.add( new Od( origin , destination ) );
			}

			origin = destination;
		}

		final int nCar = carOds.size();
		final int nPt = ptOds.size();

		if (nCar + nPt == 0) return;

		final int choice = random.nextInt( nCar + nPt );

		Od od;
		String mode;
		if (choice < nCar) {
			od = carOds.get( choice );
			mode = HitchHikingConstants.DRIVER_MODE;
		}
		else {
			od = ptOds.get( choice - nCar );
			mode = HitchHikingConstants.PASSENGER_MODE;
		}

		TripRouter.insertTrip( plan , od.o , Arrays.asList( new LegImpl( mode ) ) , od.d );
	}

	private static class Od {
		public final Activity o;
		public final Activity d;

		public Od(
				final Activity o,
				final Activity d) {
			this.o = o;
			this.d = d;
		}
	}
}

