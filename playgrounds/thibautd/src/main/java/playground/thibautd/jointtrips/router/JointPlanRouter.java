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
package playground.thibautd.jointtrips.router;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;

import playground.thibautd.jointtrips.population.DriverRoute;
import playground.thibautd.jointtrips.population.PassengerRoute;

/**
 * Based on the {@link PlanRouter}, but modifies "old" plan elements
 * rather than inserting new ones. 
 *
 * @author thibautd
 */
public class JointPlanRouter extends PlanRouter {
	private static final Logger log =
		Logger.getLogger(JointPlanRouter.class);

	//private final CarPassengerLegRouter passengerRouter = new CarPassengerLegRouter();

	public JointPlanRouter(
			final TripRouter routingHandler,
			final ActivityFacilities facilities ) {
		super( routingHandler , facilities );
	}

	public JointPlanRouter(
			final TripRouter routingHandler) {
		super( routingHandler );
	}

	@Override
	public void updatePlanElements(
			final Plan plan,
			final List<PlanElement> newPlanElements) {
		// "transmit" joint info before update
		JointRouteIterator oldPlan = new JointRouteIterator( plan.getPlanElements() );
		JointRouteIterator newPlan = new JointRouteIterator( newPlanElements ) ;

		Route oldRoute = oldPlan.nextJointRoute();
		Route newRoute = newPlan.nextJointRoute();

		while (oldRoute != null) {
			if (oldRoute instanceof DriverRoute) {
				((DriverRoute) newRoute).setPassengerIds( ((DriverRoute) oldRoute).getPassengersIds() );
			}
			else {
				((PassengerRoute) newRoute).setDriverId( ((PassengerRoute) oldRoute).getDriverId() );
			}

			oldRoute = oldPlan.nextJointRoute();
			newRoute = newPlan.nextJointRoute();
		}

		super.updatePlanElements( plan , newPlanElements );
	}

	private static class JointRouteIterator {
		private final Iterator<PlanElement> pes;

		public JointRouteIterator( final List<PlanElement> pes ) {
			this.pes = pes.iterator();
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

	private static synchronized void throwInformativeError(
			final List<PlanElement> toChange,
			final List<PlanElement> changeInfo,
			final Throwable e) {
		log.error( " ################### ERROR ################ " );
		log.error( " to update:" );
		int i=0;
		for (PlanElement pe : toChange) log.error( (++i)+": "+pe );
		log.error( " with:" );
		i=0;
		for (PlanElement pe : changeInfo) log.error( (++i)+": "+pe );

		throw new RuntimeException(
				// "problem while updating "+toChange+" of size "+toChange.size()
				//+" with "+changeInfo+" of size "+changeInfo.size(),
				e);
	}
}
