/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteNetworkRoutingModule.java
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.Facility;


/**
 * A {@link RoutingModule} which caches routes for link-to-link ODs, so that it does not
 * have to re-reoute every time. The routes may be sub-optimal.
 *
 * <br>
 * XXX: it does NOT currently use facilities, but only link ids gotten via {@link Activity#getLinkId()}!
 * <br>
 *
 * The used route is:
 * <ul>
 * <li> the route of the plan to optimise, if the OD exists (the last found route is used)
 * <li> the free flow shortest path if no route could be extracted from the plan
 * </ul>
 * @author thibautd
 */
public class FixedRouteNetworkRoutingModule implements RoutingModule {
	private static final StageActivityTypes EMPTY_CHECKER = EmptyStageActivityTypes.INSTANCE;
	private final Map< Tuple<Id, Id>, NetworkRoute> routes;

	private final Network network;
	private final String mode;
	private final boolean isFirstLinkSimulated;
	private final boolean isLastLinkSimulated;
	private final PopulationFactory populationFactory;
	private final NetworkLegRouter routingModule;
	private final TravelTime travelTime;
	private final DepartureDelayAverageCalculator tDepDelayCalc;

	public FixedRouteNetworkRoutingModule(
			final String mainMode,
			final Plan plan,
			final PopulationFactory populationFactory,
			final Network network,
			final TravelTime travelTime,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory,
			final DepartureDelayAverageCalculator tDepDelayCalc,
			final boolean isFirstLinkSimulated,
			final boolean isLastLinkSimulated) {
		this.mode = mainMode;
		this.populationFactory = populationFactory;
		this.tDepDelayCalc = tDepDelayCalc;
		this.isFirstLinkSimulated = isFirstLinkSimulated;
		this.isLastLinkSimulated = isLastLinkSimulated;

		this.network = network;
		this.travelTime = travelTime;

		routes = extractRoutes( plan , mainMode );

		// if no route is found, the shortest path without congestion will be used
		FreespeedTravelTimeAndDisutility freeFlowTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgo = leastCostPathAlgoFactory.createPathCalculator(network, freeFlowTimeCostCalc, freeFlowTimeCostCalc);
		ModeRouteFactory routeFactory = modeRouteFactory;

		this.routingModule =
			new NetworkLegRouter(
					network,
					routeAlgo,
					routeFactory);
	}

	private static Map<Tuple<Id, Id>, NetworkRoute> extractRoutes(final Plan plan , final String mode) {
		Map<Tuple<Id, Id>, NetworkRoute> routes = new HashMap<Tuple<Id, Id>, NetworkRoute>();

		OdIterator iterator = new OdIterator( plan.getPlanElements() );

		for (Od od = iterator.next(); od != null; od = iterator.next()) {
			Route route = od.leg.getRoute();

			if (od.leg.getMode().equals( mode ) && route instanceof NetworkRoute) {
				routes.put(
						extractOD( od.origin , od.destination ),
						(NetworkRoute) route.clone());
			}
		}

		return routes;
	}

	// /////////////////////////////////////////////////////////////////////////
	// routing module interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Tuple<Id, Id> od = extractOD( fromFacility , toFacility );

		NetworkRoute route = routes.get( od );

		if (route == null) {
			route = route(fromFacility, toFacility, departureTime, person);
			routes.put( od , route );
		}
		else {
			route = (NetworkRoute) route.clone();
		}

		// always "update" the travel time, even for newly created routes,
		// as routes are computed without congestion
		updateTravelTime( route , departureTime );

		Leg newLeg = populationFactory.createLeg( mode );
		newLeg.setRoute( route );
		newLeg.setDepartureTime( departureTime );
		newLeg.setTravelTime( route.getTravelTime() );

		return Arrays.asList( new PlanElement[]{ newLeg } );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EMPTY_CHECKER;
	}

	// /////////////////////////////////////////////////////////////////////////
	// Convenience methods
	// /////////////////////////////////////////////////////////////////////////
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

	private void updateTravelTime(
			final NetworkRoute route,
			final double departureTime) {

		double now = processDeparture( route.getStartLinkId() , departureTime );

		if (isFirstLinkSimulated) {
			now = processLink(
					network.getLinks().get( route.getStartLinkId() ),
					now);
		}

		now = processRouteTravelTime(
				NetworkUtils.getLinks(
					network,
					route.getLinkIds()),
				now);

		if (isLastLinkSimulated) {
			now = processLink(
					network.getLinks().get( route.getEndLinkId() ),
					now);
		}

		route.setTravelTime( now - departureTime );
	}

	private double processDeparture(final Id linkId, final double start) {
		double departureDelayEnd = start + this.tDepDelayCalc.getLinkDepartureDelay(linkId, start);
		return departureDelayEnd;
	}

	private double processRouteTravelTime(final List<Link> route, final double start) {
		double now = start;

		for (Link link : route) {
			now = this.processLink(link, now);
		}

		return now;
	}

	private double processLink(final Link link, final double start) {
		double linkEnd = start + this.travelTime.getLinkTravelTime(link, start, null, null);
		return linkEnd;
	}

	private NetworkRoute route(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {

		Leg leg = populationFactory.createLeg( mode );
		double tt = routingModule.routeLeg(
				person,
				leg,
				new FacilityWrapper( fromFacility ),
				new FacilityWrapper( toFacility ),
				departureTime);

		leg.getRoute().setTravelTime( tt );

		return (NetworkRoute) leg.getRoute();
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////

	private static class FacilityWrapper implements Activity {
		private final Facility wrapped;

		public FacilityWrapper(final Facility toWrap) {
			this.wrapped = toWrap;
		}

		@Override
		public double getEndTime() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setEndTime(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public String getType() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setType(String type) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public double getStartTime() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setStartTime(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public double getMaximumDuration() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setMaximumDuration(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public Id getLinkId() {
			return wrapped.getLinkId();
		}

		@Override
		public Id getFacilityId() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}
	}

	/**
	 * "iterator" over patterns of type Act-Leg-Act in a plan.
	 */
	private static class OdIterator {
		private final Iterator<PlanElement> pes;
		private PlanElement o = null;
		private PlanElement leg = null;
		private PlanElement d = null;

		public OdIterator(final List<PlanElement> pes) {
			this.pes = pes.iterator();
		}

		public Od next() {
			boolean wasTranslated = translate();
			if ( !wasTranslated ) {
				return null;
			}

			// search for Act-Leg-Act patterns
			while ( !(o instanceof Activity) ||
					!(leg instanceof Leg) ||
					!(d instanceof Activity) ) {
				wasTranslated = translate();
				if ( !wasTranslated ) {
					return null;
				}
			}

			return new Od( (Activity) o , (Leg) leg , (Activity) d );
		}

		private boolean translate() {
				if (!pes.hasNext()) {
					return false;
				}
				o = leg;
				leg = d;
				d = pes.next();
				return true;
		}
	}

	private static class Od {
		public final Activity origin;
		public final Leg leg;
		public final Activity destination;

		public Od(final Activity o, final Leg l, final Activity d) {
			origin = o;
			leg = l;
			destination = d;
		}
	}
}

