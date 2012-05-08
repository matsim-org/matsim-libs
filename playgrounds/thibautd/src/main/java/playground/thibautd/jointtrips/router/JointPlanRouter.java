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

import playground.thibautd.jointtrips.population.DriverRoute;
import playground.thibautd.jointtrips.population.PassengerRoute;
import playground.thibautd.router.PlanRouter;
import playground.thibautd.router.TripRouter;

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
		Iterator<PlanElement> toChange = plan.getPlanElements().iterator();
		Iterator<PlanElement> changeInfo = newPlanElements.iterator() ;

		try {
			while (toChange.hasNext()) {
				PlanElement peToChange = toChange.next();
				PlanElement peChangeInfo = changeInfo.next();

				if (peToChange instanceof Leg) {
					Leg legToChange = (Leg) peToChange;
					Leg legChangeInfo = (Leg) peChangeInfo;

					Route oldRoute = legToChange.getRoute();
					//legToChange.setMode( legChangeInfo.getMode() );
					legToChange.setDepartureTime( legChangeInfo.getDepartureTime() );
					legToChange.setTravelTime( legChangeInfo.getTravelTime() );
					legToChange.setRoute( legChangeInfo.getRoute() );

					// we do not want to loose (nor change) co-traveler information
					try {
						if (oldRoute instanceof DriverRoute) {
							((DriverRoute) legToChange.getRoute()).setPassengerIds(
									((DriverRoute) oldRoute).getPassengersIds() );
						}
						else if (oldRoute instanceof PassengerRoute) {
							((PassengerRoute) legToChange.getRoute()).setDriverId(
									((PassengerRoute) oldRoute).getDriverId() );
						}
					}
					catch (ClassCastException e) {
						throw new RuntimeException( "unexpected route type incompatibility when "
								+"updating "+legToChange.getRoute()+" with "+oldRoute );
					}
				}
			}
			if (changeInfo.hasNext()) {
				throw new RuntimeException( "incompatible size" );
			}
		}
		catch (Exception e) {
			throwInformativeError( plan.getPlanElements() , newPlanElements , e );;
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
