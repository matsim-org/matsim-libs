/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouterWrapper.java
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
package playground.thibautd.router;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Subclass of {@link PlansCalcRoute}, actually relying on a {@link PlanRouter}
 * to do all operations.
 * This is just meant to ensure backward compatibility, and new could should by
 * no mean use it!
 * <br>
 * The functionality of {@link PlansCalcRoute} has been split over the following classes:
 *
 * <ul>
 * <li> {@link TripRouterFactory} gives access to the building blocks of the routing classes
 * (cost estimators, least cost path algorithms, etc.)
 * <li> {@link TripRouter} provides methods to obtain routes individual trips (without
 * modifying the plan)
 * <li> {@link PlanRouter} provides a {@link PlanAlgorithm} which computes new routes
 * for every trip in a plan and updates the plan
 * </ul>
 *
 * @author thibautd
 */
public class PlanRouterWrapper extends PlansCalcRoute {
	private final TripRouterFactory tripRouterFactory;
	private final PlanRouter planRouter;

	public PlanRouterWrapper(
			// just used to initialise unused fields in superclass...
			final PlansCalcRouteConfigGroup group,
			final Network network,
			final TravelDisutility costCalculator,
			final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory,
			final ModeRouteFactory routeFactory,
			// this argument is the only one really used!
			final TripRouterFactory tripRouterFactory,
			final PlanRouter planRouter) {
		super(group, network, costCalculator, timeCalculator, factory, routeFactory);
		this.tripRouterFactory = tripRouterFactory;
		this.planRouter = planRouter;
	}

	public PlanRouterWrapper(
			// just used to initialise unused fields in superclass...
			final PlansCalcRouteConfigGroup group,
			final Network network,
			final TravelDisutility costCalculator,
			final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory,
			final ModeRouteFactory routeFactory,
			// this argument is the only one really used!
			final TripRouterFactory tripRouterFactory) {
		this(
			group,
			network,
			costCalculator,
			timeCalculator,
			factory,
			routeFactory,
			tripRouterFactory,
		 	new PlanRouter( tripRouterFactory.createTripRouter() ) );
	}

	//@Override
	//public void run(Person person) {
	//	// TODO Auto-generated method stub
	//	super.run(person);
	//}

	@Override
	public void run(final Plan plan) {
		planRouter.run(plan);
	}

	@Override
	protected void handlePlan(
			final Person person,
			final Plan plan) {
		run( plan );
	}

	/**
	 * This will NOT modify back the plan!
	 */
	@Override
	@Deprecated
	public double handleLeg(
			final Person person,
			final Leg leg,
			final Activity fromAct,
			final Activity toAct,
			final double depTime) {
		List<? extends PlanElement> trip = planRouter.getTripRouter().calcRoute(
				leg.getMode(),
				new ActivityWrapperFacility( fromAct ),
				new ActivityWrapperFacility( toAct ),
				depTime,
				person);

		double now = depTime;

		for (PlanElement pe : trip) {
			now = updateNow( now , pe );
		}

		if (trip.size() == 1) {
			// backward compatibility: modify argument
			Leg newLeg = (Leg) trip.get( 0 );

			leg.setDepartureTime( newLeg.getDepartureTime() );
			leg.setTravelTime( newLeg.getTravelTime() );
			leg.setRoute( newLeg.getRoute() );
		}

		return now - depTime;
	}

	@Override
	public ModeRouteFactory getRouteFactory() {
		return tripRouterFactory.getModeRouteFactory();
	}

	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof ActivityImpl ? ((ActivityImpl) act).getMaximumDuration() : Time.UNDEFINED_TIME);
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				return endTime;
			}
			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
				// use fromAct.startTime + fromAct.duration as time for routing
				return startTime + dur;
			}
			else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				return now + dur;
			}
			else {
				throw new RuntimeException("activity has neither end-time nor duration." + act);
			}
		}
		else {
			return now + ((Leg) pe).getTravelTime();
		}
	}
}

