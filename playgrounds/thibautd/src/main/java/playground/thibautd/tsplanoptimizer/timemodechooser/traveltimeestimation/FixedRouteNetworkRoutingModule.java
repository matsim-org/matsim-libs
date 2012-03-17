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
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.StageActivityTypes;
import playground.thibautd.router.StageActivityTypesImpl;
import playground.thibautd.router.TripRouterFactory;

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
	private static final StageActivityTypes EMPTY_CHECKER = new StageActivityTypesImpl( null );
	private final Map< Tuple<Id, Id>, NetworkRoute> routes;

	private final Network network;
	private final String mode;
	private final boolean isFirstLinkSimulated;
	private final boolean isLastLinkSimulated;
	private final PopulationFactory populationFactory;
	private final NetworkLegRouter routingModule;
	private final PersonalizableTravelTime travelTime;
	private final PersonalizableTravelDisutility travelCost;
	private final DepartureDelayAverageCalculator tDepDelayCalc;

	private FixedRouteNetworkRoutingModule(
			final String mainMode,
			final Plan plan,
			final PopulationFactory populationFactory,
			final TripRouterFactory routerFactory,
			final PlanCalcScoreConfigGroup scoreConfigGroup,
			final DepartureDelayAverageCalculator tDepDelayCalc,
			final boolean isFirstLinkSimulated,
			final boolean isLastLinkSimulated) {
		this.mode = mainMode;
		this.populationFactory = populationFactory;
		this.tDepDelayCalc = tDepDelayCalc;
		this.isFirstLinkSimulated = isFirstLinkSimulated;
		this.isLastLinkSimulated = isLastLinkSimulated;

		this.network = routerFactory.getNetwork();
		this.travelTime = routerFactory.getTravelTimeCalculatorFactory().createTravelTime();
		this.travelCost = routerFactory.getTravelCostCalculatorFactory().createTravelDisutility( travelTime , scoreConfigGroup );

		routes = extractRoutes( plan );

		// if no route is found, the shortest path without congestion will be used
		FreespeedTravelTimeAndDisutility freeFlowTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		LeastCostPathCalculatorFactory leastCostPathAlgoFactory = routerFactory.getLeastCostPathCalculatorFactory();
		LeastCostPathCalculator routeAlgo = leastCostPathAlgoFactory.createPathCalculator(network, freeFlowTimeCostCalc, freeFlowTimeCostCalc);
		ModeRouteFactory routeFactory = routerFactory.getModeRouteFactory();

		this.routingModule =
			new NetworkLegRouter(
					network,
					routeAlgo,
					routeFactory);
	}

	private static Map<Tuple<Id, Id>, NetworkRoute> extractRoutes(final Plan plan) {
		Map<Tuple<Id, Id>, NetworkRoute> routes = new HashMap<Tuple<Id, Id>, NetworkRoute>();

		// assume strict act/leg alternance.
		// if not, abort and only keep found routes.
		try {
			Iterator<PlanElement> elementIterator = plan.getPlanElements().iterator();
			Activity origin = (Activity) elementIterator.next();

			while ( elementIterator.hasNext() ) {
				Leg leg = (Leg) elementIterator.next();
				Activity destination = (Activity) elementIterator.next();

				Route route = leg.getRoute();

				if (route instanceof NetworkRoute) {
					routes.put(
							extractOD( origin , destination ),
							(NetworkRoute) route.clone());
				}

				origin = destination;
			}
		}
		catch (ClassCastException e) {
			return routes;
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
		updateTravelTime( route , departureTime , person );

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
			final double departureTime,
			final Person person) {
		travelTime.setPerson( person );
		travelCost.setPerson( person );

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
		double linkEnd = start + this.travelTime.getLinkTravelTime(link, start);
		return linkEnd;
	}

	private NetworkRoute route(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		travelTime.setPerson( person );
		travelCost.setPerson( person );

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
	public static RoutingModuleFactory getFactory(
			final Plan plan,
			final PlanCalcScoreConfigGroup configGroup,
			final PopulationFactory populationFactory,
			final DepartureDelayAverageCalculator tDepDelayCalc,
			final boolean isFirstLinkSimulated,
			final boolean isLastLinkSimulated) {
		return new Factory(
				plan, configGroup,
				populationFactory,
				tDepDelayCalc,
				isFirstLinkSimulated,
				isLastLinkSimulated );
	}

	private static class Factory implements RoutingModuleFactory {
		private final  PlanCalcScoreConfigGroup conf;
		private final Plan plan;
		private final PopulationFactory popFact;
		private final DepartureDelayAverageCalculator tDepDelayCalc;
		private final boolean isFirstLinkSimulated;
		private final boolean isLastLinkSimulated;

		public Factory(
				final Plan plan,
				final PlanCalcScoreConfigGroup conf,
				final PopulationFactory fact,
				final DepartureDelayAverageCalculator tDepDelayCalc,
				final boolean isFirstLinkSimulated,
				final boolean isLastLinkSimulated) {
			this.conf = conf;
			this.plan = plan;
			this.popFact = fact;
			this.tDepDelayCalc = tDepDelayCalc;
			this.isFirstLinkSimulated = isFirstLinkSimulated;
			this.isLastLinkSimulated = isLastLinkSimulated;
		}

		@Override
		public RoutingModule createModule(
				final String mainMode,
				final TripRouterFactory factory) {
			return new FixedRouteNetworkRoutingModule(
					mainMode,
					plan,
					popFact,
					factory,
					conf,
					tDepDelayCalc,
					isFirstLinkSimulated,
					isLastLinkSimulated );
		}
	}

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
}

