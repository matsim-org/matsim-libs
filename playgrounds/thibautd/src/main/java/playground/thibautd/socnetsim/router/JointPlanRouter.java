/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanRouter.java
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
package playground.thibautd.socnetsim.router;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * Based on the {@link PlanRouter}, but transmits passenger information
 * in the new plan elements.
 *
 * Note that this implies that using the trip router alone will loose
 * this information!
 *
 * @author thibautd
 */
public class JointPlanRouter implements PlanAlgorithm {
	private final PlanRouter delegate;
	
	public JointPlanRouter(
			final TripRouter routingHandler,
			final ActivityFacilities facilities ) {
		delegate = new PlanRouter( routingHandler , facilities );
	}

	@Override
	public void run( final Plan plan ) {
		JointRouteIterator oldPlan = new JointRouteIterator( plan.getPlanElements() );
		delegate.run( plan );
		// "transmit" joint info before returning
		JointRouteIterator newPlan = new JointRouteIterator( plan.getPlanElements() ) ;

		Route oldRoute = oldPlan.nextJointRoute();
		Route newRoute = newPlan.nextJointRoute();

		while (oldRoute != null) {
			if (oldRoute instanceof DriverRoute) {
				((DriverRoute) newRoute).setPassengerIds(
					((DriverRoute) oldRoute).getPassengersIds() );
			}
			else {
				((PassengerRoute) newRoute).setDriverId(
					((PassengerRoute) oldRoute).getDriverId() );
			}

			oldRoute = oldPlan.nextJointRoute();
			newRoute = newPlan.nextJointRoute();
		}

		assert oldRoute == null;
		assert newRoute == null;
	}

	public TripRouter getTripRouter() {
		return delegate.getTripRouter();
	}

	private static class JointRouteIterator {
		private final Iterator<PlanElement> pes;

		public JointRouteIterator( final List<PlanElement> pes ) {
			this.pes = new ArrayList<PlanElement>( pes ).iterator();
		}

		public Route nextJointRoute() {
			while (pes.hasNext()) {
				PlanElement pe = pes.next();
				if (pe instanceof Leg &&
						(((Leg) pe).getRoute() instanceof DriverRoute ||
						 ((Leg) pe).getRoute() instanceof PassengerRoute)) {
					return ((Leg) pe).getRoute();
				}
			}
			return null;
		}
	}
}
