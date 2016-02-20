/* *********************************************************************** *
 * project: org.matsim.*
 * DriverRouteHandler.java
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
package playground.thibautd.hitchiking.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.Facility;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.population.HitchHikingDriverRoute;
import playground.thibautd.hitchiking.qsim.events.PassengerDepartsWithDriverEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handler for the driver mode.
 * This is where the magic happens.
 * @author thibautd
 */
public class DriverRouteHandler implements HitchHikingHandler {
	private static final Logger log =
		Logger.getLogger(DriverRouteHandler.class);

	/**
	 * Maximum number of passengers per driver. 
	 */
	public static final int N_PASSENGERS = 3;
	private final HitchHikingDriverRoute route;
	private final PassengerQueuesManager queuesManager;
	private final TripRouter router;
	private final HitchHikerAgent agent;
	private final EventsManager events;
	private final Network network;
	private final double costOfDistance;

	private Stage stage = Stage.ACCESS;
	private double now;

	private Collection<MobsimAgent> passengers = null;
	private Id currentDestination;
	private Iterator<Id<Link>> routeToNextDest;
	private Id currentIdInRoute;
	private Id nextIdInRoute;

	public DriverRouteHandler(
			final Network network,
			final HitchHikerAgent agent,
			final TripRouter router,
			final PassengerQueuesManager manager,
			final EventsManager events,
			final HitchHikingDriverRoute route,
			final double costOfDistance,
			final double now) {
		this.agent = agent;
		this.router = router;
		this.queuesManager = manager;
		this.route = route;
		this.now = now;
		this.events = events;
		this.costOfDistance = costOfDistance;
		this.network = network;

		performAccess();
	}

	private void performAccess() {
		currentDestination = route.getPickUpLinkId();
		currentIdInRoute = route.getStartLinkId();
		routeToNextDest = route( currentIdInRoute , currentDestination ).getLinkIds().iterator();
		nextIdInRoute = nextIdInRoute();
	}

	@Override
	public Id getCurrentLinkId() {
		return currentIdInRoute;
	}

	@Override
	public Id chooseNextLinkId() {
		return nextIdInRoute;
	}

	@Override
	public Id getDestinationLinkId() {
		return currentDestination;
	}

	@Override
	public State getState() {
		switch (stage) {
			case ACCESS:
			case CAR_POOL:
			case EGRESS:
				return State.LEG;
			case PICK_UP:
			case DROP_OFF:
				return State.ACTIVITY;
			default:
				throw new RuntimeException( "What is that? "+stage );
		}
	}

	@Override
	public double getActivityEndTime() {
		return now;
	}

	@Override
	public boolean endActivityAndComputeNextState(double now1) {
		return nextState( now1 );
	}

	@Override
	public boolean endLegAndComputeNextState(double now1) {
		return nextState( now1 );
	}

	private boolean nextState(final double now1) {
		stage = stage.next();
		this.now = now1;

		if (stage == null) return false;

		switch (stage) {
			case PICK_UP:
				return performPickUp( );
			case CAR_POOL:
				return performCarPool( );
			case DROP_OFF:
				return performDropOff( );
			case EGRESS:
				return performEgress( );
			default:
				throw new IllegalStateException( "unexpected: "+stage );
		}
	}

	private boolean performCarPool() {
		// everything is done in the pick-up
		return true;
	}

	private boolean performDropOff() {
		if (passengers != null) {
			queuesManager.arrangePassengersArrivals( passengers , getCurrentLinkId() , now );
		}

		if (currentIdInRoute.equals( route.getEndLinkId() )) {
			return false;
		}
		else {
			events.processEvent(
					new PersonArrivalEvent(now, agent.getId(), currentDestination, HitchHikingConstants.DRIVER_MODE));

			return true;
		}
	}

	private boolean performEgress() {
		currentDestination = route.getEndLinkId();
		routeToNextDest = route( currentIdInRoute , currentDestination ).getLinkIds().iterator();
		nextIdInRoute = nextIdInRoute();
		return true;
	}

	private boolean performPickUp() {
		final Id pickUpLink =  route.getPickUpLinkId();
		events.processEvent(
				new PersonArrivalEvent(now, agent.getId(), pickUpLink, TransportMode.car));

		Tuple<Id, Collection<MobsimAgent>> destAndPassengers =
			queuesManager.getPassengersFromFirstNonEmptyQueue(
					now,
					route.getPickUpLinkId(),
					route.getDropOffLinksIds(),
					N_PASSENGERS);

		if (destAndPassengers != null) {
			passengers = destAndPassengers.getSecond();
			currentDestination = destAndPassengers.getFirst();		
			NetworkRoute r = route(
					route.getPickUpLinkId(),
					currentDestination);
			routeToNextDest = r.getLinkIds().iterator();

			// XXX: this assumes no cost of distance for hitch hiking in scoring f.
			double dist = RouteUtils.calcDistanceExcludingStartEndLink( r , network ) +
				network.getLinks().get( r.getEndLinkId() ).getLength();
			double malusPerPerson = (dist * costOfDistance) /
									(passengers.size() + 1d);

			for (MobsimAgent p : passengers) {
				// fire event for each passenger
				events.processEvent(
						new PassengerDepartsWithDriverEvent(
							now,
							p.getId(),
							agent.getId(),
							pickUpLink));
				if (log.isTraceEnabled()) log.trace( "passenger "+p.getId()+" gets bonus "+malusPerPerson );
				events.processEvent(
						new PersonMoneyEvent(now, p.getId(), malusPerPerson));
			}
			if (log.isTraceEnabled()) log.trace( "driver "+agent.getId()+" gets bonus "+malusPerPerson );
			events.processEvent(
					new PersonMoneyEvent(now, agent.getId(), malusPerPerson));
		}
		else {
			passengers = null;
			currentDestination = route.getEndLinkId();
			routeToNextDest =
				route(
					route.getPickUpLinkId(),
					currentDestination).getLinkIds().iterator();
		}
		currentIdInRoute = route.getPickUpLinkId();
		nextIdInRoute = nextIdInRoute();

		return true;
	}

	private NetworkRoute route(final Id o , final Id d) {
		List<? extends PlanElement> trip = 
			router.calcRoute(
					TransportMode.car,
					new LinkFacility( o ),
					new LinkFacility( d ),
					now,
					agent.getPerson());

		Leg l = (Leg) trip.get( 0 );
		return (NetworkRoute) l.getRoute();
	}

	@Override
	public String getMode() {
		switch( stage ) {
			case ACCESS:
			case EGRESS:
				return TransportMode.car;
			case CAR_POOL:
				return HitchHikingConstants.DRIVER_MODE;
			default:
			 throw new IllegalStateException( "unexpected "+stage );
		}
	}

	@Override
	public void notifyMoveOverNode(final Id newLinkId) {
		if (nextIdInRoute.equals( newLinkId )) {
			currentIdInRoute = newLinkId;
			nextIdInRoute = nextIdInRoute();
		}
		else {
			throw new IllegalStateException();
		}
	}

	private Id nextIdInRoute() {
		return  routeToNextDest.hasNext() ? routeToNextDest.next() :
			currentIdInRoute.equals( currentDestination ) ?
				null :
				currentDestination;
	}
}

class LinkFacility implements Facility {
	private final Id link;

	public LinkFacility(final Id link) {
		this.link = link;
	}

	@Override
	public Coord getCoord() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id getLinkId() {
		return link;
	}
}
