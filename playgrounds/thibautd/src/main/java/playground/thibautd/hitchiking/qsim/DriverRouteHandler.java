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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.population.HitchHikingDriverRoute;
import playground.thibautd.router.TripRouter;

/**
 * @author thibautd
 */
public class DriverRouteHandler implements HitchHikingHandler {
	public static final int N_PASSENGERS = 3;
	private final HitchHikingDriverRoute route;
	private final PassengerQueuesManager queuesManager;
	private final TripRouter router;
	private final HitchHikerAgent agent;

	private Stage stage = Stage.ACCESS;
	private double now;

	private Collection<MobsimAgent> passengers = null;
	private Id currentDestination;
	private Iterator<Id> routeToNextDest;
	private Id currentIdInRoute;
	private Id nextIdInRoute;

	public DriverRouteHandler(
			final HitchHikerAgent agent,
			final TripRouter router,
			final PassengerQueuesManager manager,
			final HitchHikingDriverRoute route,
			final double now) {
		this.agent = agent;
		this.router = router;
		this.queuesManager = manager;
		this.route = route;
		this.now = now;

		performAccess();
	}

	private void performAccess() {
		currentDestination = route.getPickUpLinkId();
		currentIdInRoute = route.getStartLinkId();
		routeToNextDest = route( currentIdInRoute , currentDestination );
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
	public boolean endActivityAndComputeNextState(double now) {
		return nextState( now );
	}

	@Override
	public boolean endLegAndComputeNextState(double now) {
		return nextState( now );
	}

	private boolean nextState(final double now) {
		stage = stage.next();
		this.now = now;

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
		}

		throw new RuntimeException( "unexpected "+stage );
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
			return true;
		}
	}

	private boolean performEgress() {
		currentDestination = route.getEndLinkId();
		routeToNextDest = route( currentIdInRoute , currentDestination );
		nextIdInRoute = nextIdInRoute();
		return true;
	}

	private boolean performPickUp() {
		Tuple<Id, Collection<MobsimAgent>> destAndPassengers =
			queuesManager.getPassengersFromFirstNonEmptyQueue(
					now,
					route.getPickUpLinkId(),
					route.getDropOffLinksIds(),
					N_PASSENGERS);

		if (destAndPassengers != null) {
			passengers = destAndPassengers.getSecond();
			currentDestination = destAndPassengers.getFirst();		
			routeToNextDest =
				route(
					route.getPickUpLinkId(),
					currentDestination);
		}
		else {
			passengers = null;
			currentDestination = route.getEndLinkId();
			routeToNextDest =
				route(
					route.getPickUpLinkId(),
					currentDestination);
		}
		currentIdInRoute = route.getPickUpLinkId();
		nextIdInRoute = nextIdInRoute();

		return true;
	}

	private Iterator<Id> route(final Id o , final Id d) {
		List<? extends PlanElement> trip = 
			router.calcRoute(
					TransportMode.car,
					new LinkFacility( o ),
					new LinkFacility( d ),
					now,
					agent.getPerson());

		Leg l = (Leg) trip.get( 0 );
		NetworkRoute r = (NetworkRoute) l.getRoute();
		return r.getLinkIds().iterator();
	}

	@Override
	public String getMode() {
		switch( stage ) {
			case ACCESS:
			case EGRESS:
				return TransportMode.car;
			case CAR_POOL:
				return HitchHikingConstants.DRIVER_MODE;
		}

		throw new IllegalStateException( ""+stage );
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
