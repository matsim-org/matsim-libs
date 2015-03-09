/* *********************************************************************** *
 * project: org.matsim.*
 * FixedTransitRouteRoutingModule.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


/**
 * A {@link RoutingModule} which caches routes for link-to-link ODs, so that it does not
 * have to re-reoute every time. The routes may be sub-optimal.
 * <br>
 * In an ideal world, it would use the travel time estimators and the network used by the
 * "base" routing module. However, as the transit routes do not translate easily to the
 * transit network router, the schedule is used directly.
 * <br>
 *
 * The used route is:
 * <ul>
 * <li> the route of the plan to optimise, if the OD exists (the last found route is used)
 * <li> the route for the first departure time encountered, if the OD does not exist yet.
 * </ul>
 *
 * Note that using a fixed route for the whole day
 * is a rather drastic approach, which may cause problems,
 * for instance when the first found route is a night line for a trip which
 * optimal start time would be during the day, but more complex solutions
 * (route per time bin, reset the memory every k TS iterations) were not tested.
 * <br>
 *
 * It modifies and returns the same instances over and over, so it is not thread
 * safe.
 * @author thibautd
 */
public class FixedTransitRouteRoutingModule implements RoutingModule {
	private final boolean useFixedRoutes;
	private final static double MIDNIGHT = 24.0*3600;
	private final TransitSchedule schedule;
	private final TransitRouterWrapper module;
	private final Map< Tuple<Id, Id> , List<? extends PlanElement> > routes;
	private final Map< TransitRoute , double[] > sortedDepartures = new HashMap< TransitRoute , double[] >();

	// /////////////////////////////////////////////////////////////////////////
	// Construction
	// /////////////////////////////////////////////////////////////////////////

	public FixedTransitRouteRoutingModule(
			final Plan plan,
			final TransitSchedule schedule,
			final TransitRouterWrapper module) {
		this( plan , schedule , module , true );
	}

	/**
	 * @param plan the plan to get initial fixed routes from
	 * @param schedule the transit schedule
	 * @param module the module to use to compute yet unkown routes
	 * @param useFixedRoutes whether to cache routes per OD or not.
	 * Not using is takes a great deal of time, but may be the only
	 * way to get correct results in some scenarios. 
	 * If this is false, this module basically wrapps the base module
	 * and adds travel time information to all legs.
	 */
	public FixedTransitRouteRoutingModule(
			final Plan plan,
			final TransitSchedule schedule,
			final TransitRouterWrapper module,
			final boolean useFixedRoutes) {
		this.module = module;
		this.schedule = schedule;
		this.useFixedRoutes = useFixedRoutes;
		this.routes = analysePlan(
				module,
				plan,
				useFixedRoutes);
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Tuple<Id, Id> od = extractOD(fromFacility, toFacility);
		List<? extends PlanElement> route = routes.get( od );

		if (route == null) {
			route = module.calcRoute(
					fromFacility,
					toFacility,
					departureTime,
					person);

			if (useFixedRoutes) {
				routes.put( od , route );
			}
		}

		// always update, as the "pure" router does not sets al required fields
		updateTimeInfo(
		 		departureTime,
		 		route);

		return route;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return module.getStageActivityTypes();
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static Map<Tuple<Id, Id>, List<? extends PlanElement>> analysePlan(
			final TransitRouterWrapper module,
			final Plan plan,
			final boolean useFixedRoutes) {
		Map< Tuple<Id, Id>, List<? extends PlanElement> > routes = new HashMap<Tuple<Id,Id>, List<? extends PlanElement>>();

		if (useFixedRoutes) {
			StageActivityTypes checker = module.getStageActivityTypes();
			List<PlanElement> currentTrip = new ArrayList<PlanElement>();

			Activity origin = null;
			for (PlanElement currentElement : plan.getPlanElements()) {
				if (currentElement instanceof Activity) {
					Activity act = (Activity) currentElement;

					if (checker.isStageActivity( act.getType() )) {
						currentTrip.add( act );
					}
					else {
						if (currentTrip.size() > 0) {
							if (isTransitTrip( currentTrip )) {
								routes.put( 
										extractOD( origin , act ),
										currentTrip);
							}
							currentTrip = new ArrayList<PlanElement>();
						}

						origin = act;
					}
				}
				else if (currentElement instanceof Leg) {
					currentTrip.add( currentElement );
				}
				else {
					throw new RuntimeException( "unknown PlanElement implementation "+currentElement.getClass() );
				}
			}
		}

		return routes;
	}

	private static boolean isTransitTrip(final List<? extends PlanElement> currentTrip) {
		return ((Leg) currentTrip.get( 0 )).getMode().equals( TransportMode.transit_walk );
	}

	private static Tuple<Id, Id> extractOD(final Facility o, final Facility d) {
		return new Tuple<Id, Id>(
				o.getLinkId(),
				d.getLinkId());
	}

	private static Tuple<Id, Id> extractOD(final Activity o, final Activity d) {
		return new Tuple<Id, Id>(
				o.getLinkId(),
				d.getLinkId());
	}

	private void updateTimeInfo(
			final double departureTime,
			final List<? extends PlanElement> trip) {
		double now = departureTime;

		for (PlanElement pe : trip) {
			if (pe instanceof Activity) {
				((Activity) pe).setStartTime( Time.UNDEFINED_TIME );
				((Activity) pe).setEndTime( Time.UNDEFINED_TIME );
			}
			else if (pe instanceof Leg) {
				Route route = ((Leg) pe).getRoute();

				if (route instanceof ExperimentalTransitRoute) {
					ExperimentalTransitRoute castedRoute = (ExperimentalTransitRoute) route;

					TransitLine transitLine = schedule.getTransitLines().get( castedRoute.getLineId() );
					TransitRoute transitRoute = transitLine.getRoutes().get( castedRoute.getRouteId() );
					TransitRouteStop departureStop = transitRoute.getStop( schedule.getFacilities().get( castedRoute.getAccessStopId() ) );
					TransitRouteStop arrivalStop = transitRoute.getStop( schedule.getFacilities().get( castedRoute.getEgressStopId() ) );

					double arrival = getArrivalTime(
							transitRoute,
							departureStop,
							arrivalStop,
							now);

					// update time info
					castedRoute.setTravelTime( arrival - now );
					((Leg) pe).setDepartureTime( now );
					((Leg) pe).setTravelTime( arrival - now );

					now = arrival;
				}
				else if (route instanceof GenericRoute) {
				  ((Leg) pe).setDepartureTime( now );
				  //((Leg) pe).setTravelTime( route.getTravelTime() );
				  //now += route.getTravelTime();
				  now += ((Leg) pe).getTravelTime();
				}
				else {
					throw new RuntimeException(
							"unexpected route implementation "+route.getClass()+
							" in leg "+pe+" for trip "+trip+". Cannot be sure that"+
							" the travel time re-estimation is correct." );
				}

			}
			else {
				throw new RuntimeException(
						"unexpected PlanElement implementation "+pe.getClass()+
						" in trip "+trip );
			}
		}
	}

	private double getDepartureTime(
			final TransitRoute route,
			final TransitRouteStop depStop,
			final double depTime) {
		// this is the earliest departure time of a vehicle, from its
		// start stop (a vehicle starting before would leave the stop before
		// the requested departure time)
		double earliestDepartureTime = depTime - depStop.getDepartureOffset();

		if (earliestDepartureTime >= MIDNIGHT) {
			earliestDepartureTime = earliestDepartureTime % MIDNIGHT;
		}

		double[] cache = this.sortedDepartures.get(route);
		if (cache == null) {
			cache = new double[route.getDepartures().size()];
			int i = 0;
			for (Departure dep : route.getDepartures().values()) {
				cache[i++] = dep.getDepartureTime();
			}
			Arrays.sort(cache);
			this.sortedDepartures.put(route, cache);
		}
		int pos = Arrays.binarySearch(cache, earliestDepartureTime);
		if (pos < 0) {
			pos = -(pos + 1);
		}
		if (pos >= cache.length) {
			pos = 0; // there is no later departure time, take the first in the morning
		}
		double bestDepartureTime = cache[pos];

		// we now have the departure time of the vehicle at its origin:
		// convert in the departure time at the stop
		bestDepartureTime += depStop.getDepartureOffset();
		while (bestDepartureTime < depTime) {
			bestDepartureTime += MIDNIGHT;
		}
		return bestDepartureTime;
	}
	
	private double getArrivalTime(
			final TransitRoute route,
			final TransitRouteStop fromStop,
			final TransitRouteStop toStop,
			final double depTime) {
		double vehicleDeparture = getDepartureTime( route , fromStop , depTime );

		double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
		double inVehicleTravelTime = arrivalOffset - fromStop.getDepartureOffset();
		double arrivalTime = vehicleDeparture + inVehicleTravelTime;
		if (arrivalTime < depTime) {
			// ( this can only happen, I think, when ``bestDepartureTime'' is after midnight but ``time'' was before )
			arrivalTime += MIDNIGHT;
		}

		return arrivalTime;
	}
}

