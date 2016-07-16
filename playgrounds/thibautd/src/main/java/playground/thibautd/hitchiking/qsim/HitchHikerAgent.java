/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikerAgent.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.population.HitchHikingDriverRoute;

import java.util.List;

/**
 * Agent for simulating casual car-pooling.
 * Rather than using an engine, the agent exposes on-the-fly
 * created activities and legs, derived from the route and the events.
 *
 * @author thibautd
 */
public class HitchHikerAgent implements MobsimDriverPassengerAgent , HasPerson {
	/**
	 * the "basic" delegate, used outside of hitch-hiking trips
	 */
	private final MobsimDriverPassengerAgent delegate;
	private final TripRouter router;
	private final PassengerQueuesManager manager;
	private final EventsManager events;
	private final Network network;
	// XXX this should not be here but in the scoring function
	private final double costOfDistance;

	/**
	 * hitch-hiking-specific delegate.
	 * One new instance is created for each trip, and is responsible for
	 * exposing "fake" activities and legs.
	 */ 
	private HitchHikingHandler hitchHiker = null;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public HitchHikerAgent(
			final MobsimDriverPassengerAgent delegate,
			final Network network,
			final TripRouter router,
			final PassengerQueuesManager manager,
			final EventsManager events,
			final double costOfDistance) {
		this.delegate = delegate;
		this.router = router;
		this.manager = manager;
		this.events = events;
		this.costOfDistance = costOfDistance;
		this.network = network;
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific (basically, "delegate choosing" methods.
	// Nothing exciting is done here.)
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getCurrentLinkId() {
		return hitchHiker != null ?
			hitchHiker.getCurrentLinkId() :
			delegate.getCurrentLinkId();
	}

	@Override
	public Id chooseNextLinkId() {
		return hitchHiker != null ?
			hitchHiker.chooseNextLinkId() :
			delegate.chooseNextLinkId();
	}

	@Override
	public Id getDestinationLinkId() {
		return hitchHiker != null ?
			hitchHiker.getDestinationLinkId() :
			delegate.getDestinationLinkId();
	}

	@Override
	public State getState() {
		return hitchHiker != null ?
			hitchHiker.getState() :
			delegate.getState();
	}

	@Override
	public double getActivityEndTime() {
		return hitchHiker != null ?
			hitchHiker.getActivityEndTime() :
			delegate.getActivityEndTime();
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		if ( hitchHiker != null && !hitchHiker.endActivityAndComputeNextState( now ) ) {
			hitchHiker = null;
		}

		if ( hitchHiker == null ) {
			delegate.endActivityAndComputeNextState(now);
			analyseCurrentStateAndSetStatus( now );
		}
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		if ( hitchHiker != null && !hitchHiker.endLegAndComputeNextState( now ) ) {
			// FIXME: this generates a "traveled" event!
			delegate.notifyArrivalOnLinkByNonNetworkMode( hitchHiker.getCurrentLinkId() );
			hitchHiker = null;
		}

		if ( hitchHiker == null ) {
			delegate.endLegAndComputeNextState(now);
			analyseCurrentStateAndSetStatus( now );
		}
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return chooseNextLinkId() == null;
	}


	@Override
	public String getMode() {
		return hitchHiker != null ?
			hitchHiker.getMode() :
			delegate.getMode();
	}

	private void analyseCurrentStateAndSetStatus(final double now) {
		if ( !getState().equals( State.LEG ) ) return;

		Leg leg = (Leg) ((PlanAgent) delegate).getCurrentPlanElement();
		Route route = leg.getRoute();

		if (leg.getMode().equals( HitchHikingConstants.DRIVER_MODE )) {
			hitchHiker = new DriverRouteHandler(
					network,
					this,
					router,
					manager,
					events,
					(HitchHikingDriverRoute) route,
					costOfDistance,
					now);
		}
		else if (leg.getMode().equals( HitchHikingConstants.PASSENGER_MODE )) {
			hitchHiker = new PassengerRouteHandler( route );
		}
		else {
			hitchHiker = null;
		}
	}

	@Override
	public Id getPlannedVehicleId() {
		//return delegate.getPlannedVehicleId();
		return getId();
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		if (hitchHiker != null) {
			hitchHiker.notifyMoveOverNode( newLinkId );
		}
		else {
			delegate.notifyMoveOverNode(newLinkId);
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// pure delegation
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return delegate.getId();
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		delegate.setVehicle(veh);
	}

	@Override
	public MobsimVehicle getVehicle() {
		return delegate.getVehicle();
	}

	@Override
	public void setStateToAbort(double now) {
		delegate.setStateToAbort(now);
	}

	@Override
	public Double getExpectedTravelTime() {
		return delegate.getExpectedTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {
        return delegate.getExpectedTravelDistance();
    }

    @Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public Person getPerson() {
		return ((HasPerson) delegate).getPerson();
	}

	@Override
	public boolean getEnterTransitRoute(
			final TransitLine line,
			final TransitRoute transitRoute,
			final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		return delegate.getEnterTransitRoute(line, transitRoute, stopsToCome, null);
	}

	@Override
	public boolean getExitAtStop(
			final TransitStopFacility stop) {
		return delegate.getExitAtStop(stop);
	}

	@Override
	public Id getDesiredAccessStopId() {
		return delegate.getDesiredAccessStopId();
	}
	
	@Override
	public Id getDesiredDestinationStopId() {
		return this.delegate.getDesiredDestinationStopId();
	}

	@Override
	public double getWeight() {
		return delegate.getWeight();
	}

	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.delegate.getCurrentFacility();
	}

	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.delegate.getDestinationFacility();
	}
}
