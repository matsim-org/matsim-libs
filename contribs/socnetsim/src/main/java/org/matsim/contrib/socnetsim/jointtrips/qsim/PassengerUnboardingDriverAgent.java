/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerUnboardingDriverAgent.java
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
package org.matsim.contrib.socnetsim.jointtrips.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.contrib.socnetsim.qsim.QVehicleProvider;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.utils.IdentifiableCollectionsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PassengerUnboardingDriverAgent implements MobsimDriverAgent, PlanAgent, PassengerAgent, PTPassengerAgent, HasPerson {
	private final MobsimDriverAgent delegate;
	private final PTPassengerAgent ptDelegate;
	private final PlanAgent planDelegate;
	private final QVehicleProvider vehicleProvider;
	private final InternalInterface internalInterface;

	private final List<PassengerAgent> passengersToBoard = new ArrayList<PassengerAgent>();

	public PassengerUnboardingDriverAgent(
			final MobsimAgent delegate,
			final QVehicleProvider vehicleProvider,
			final InternalInterface internalInterface) {
		if ( delegate == null ) throw new IllegalArgumentException( "delegate cannot be null" );
		this.delegate = (MobsimDriverAgent) delegate;
		this.ptDelegate = delegate instanceof PTPassengerAgent ? (PTPassengerAgent) delegate : null;
		this.planDelegate = (PlanAgent) delegate;
		this.vehicleProvider = vehicleProvider;
		this.internalInterface = internalInterface;
	}

	// /////////////////////////////////////////////////////////////////////////
	// modified methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void endLegAndComputeNextState(final double now) {
		final String mode = delegate.getMode();

		if ( mode == null ) {
			throw new IllegalStateException(
					"delegate "+delegate+
					" for agent "+delegate.getId()+
					" returned a null mode. Probably in a wrong state: "
					+delegate.getState()+" with current plan element "+getCurrentPlanElement() );
		}

		if ( mode.equals( JointActingTypes.DRIVER ) ) {
			if ( !passengersToBoard.isEmpty() ) {
				assert ((NetworkRoute) ((Leg) getCurrentPlanElement()).getRoute()).getLinkIds().isEmpty();
				boardPassengers();
				assert passengersToBoard.isEmpty();
			}

			final MobsimVehicle vehicle = vehicleProvider.getVehicle( delegate.getPlannedVehicleId() );
			final Id linkId = delegate.getCurrentLinkId();
			final Collection<PassengerAgent> passengersToUnboard = new ArrayList<PassengerAgent>();
			assert vehicle != null;
			for ( PassengerAgent p : vehicle.getPassengers() ) {
				if ( p.getDestinationLinkId().equals( linkId ) ) {
					passengersToUnboard.add( p );
				}
			}

			final EventsManager events = ((QSim) internalInterface.getMobsim()).getEventsManager();
			for (PassengerAgent p : passengersToUnboard) {
				assert p != this;
				assert !p.getId().equals( getId() );

				vehicle.removePassenger( p );
				((MobsimAgent) p).notifyArrivalOnLinkByNonNetworkMode( delegate.getCurrentLinkId() );
				((MobsimAgent) p).endLegAndComputeNextState( now );
				events.processEvent(
						new PersonLeavesVehicleEvent(now, p.getId(), vehicle.getId()));
				internalInterface.arrangeNextAgentState( (MobsimAgent) p );
			}
		}

		delegate.endLegAndComputeNextState( now );
	}

	@Override
	public void notifyMoveOverNode(final Id newLinkId) {
		assert passengersToBoard.isEmpty();
		delegate.notifyMoveOverNode(newLinkId);
	}

	private void boardPassengers() {
		// using the vehicle provider might give access to a vehicle which is
		// used by somebody else, if the sequence is incorrect.
		// Using getVehicle() looks safer, as it should fail if the vehicle
		// is somewhere else.
		// It remains however problematic, as it is not clear whether agents will continue
		// to be given the vehicle or not...
		final MobsimVehicle vehicle = getVehicle();// vehicleProvider.getVehicle( delegate.getPlannedVehicleId() );
		final EventsManager events = ((QSim) internalInterface.getMobsim()).getEventsManager();
		for ( PassengerAgent passenger : passengersToBoard ) {
			assert passenger.getCurrentLinkId().equals( getCurrentLinkId() ) : passenger+" is at "+passenger.getCurrentLinkId()+" instead of "+getCurrentLinkId()+" for driver "+this;
			assert ((Leg) getCurrentPlanElement()).getMode().equals( JointActingTypes.DRIVER ) : getCurrentPlanElement();
			assert ((DriverRoute) ((Leg) getCurrentPlanElement()).getRoute()).getPassengersIds().contains( passenger.getId() ) :
				passenger+" not in "+((DriverRoute) ((Leg) getCurrentPlanElement()).getRoute()).getPassengersIds()+" for driver "+this;

			final boolean isAdded = vehicle.addPassenger( passenger );
			if ( !isAdded ) {
				// do not know how to handle that...
				throw new RuntimeException( passenger+" could not be added to vehicle "+vehicle );
			}

			events.processEvent(
					new PersonEntersVehicleEvent(internalInterface.getMobsim().getSimTimer().getTimeOfDay(), passenger.getId(), vehicle.getId()));
		}
		passengersToBoard.clear();

		final DriverRoute dr = (DriverRoute) ((Leg) getCurrentPlanElement()).getRoute();
		assert vehicle.getPassengers().size() == dr.getPassengersIds().size() &&
			IdentifiableCollectionsUtils.containsAll( dr.getPassengersIds() , vehicle.getPassengers() ) :
				vehicle.getPassengers()+" != "+dr.getPassengersIds()+" for driver "+this;
	}

	public void addPassengerToBoard(final PassengerAgent passenger) {
		passengersToBoard.add( passenger );
	}

	@Override
	public void setVehicle(final MobsimVehicle veh) {
		delegate.setVehicle(veh);
		if ( !passengersToBoard.isEmpty() ) {
			boardPassengers();
			assert passengersToBoard.isEmpty();
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
	public Id getCurrentLinkId() {
		return delegate.getCurrentLinkId();
	}

	@Override
	public MobsimVehicle getVehicle() {
		return delegate.getVehicle();
	}

	@Override
	public Id getDestinationLinkId() {
		return delegate.getDestinationLinkId();
	}

	@Override
	public Id chooseNextLinkId() {
		return delegate.chooseNextLinkId();
	}

	@Override
	public State getState() {
		return delegate.getState();
	}

	@Override
	public Id getPlannedVehicleId() {
		return delegate.getPlannedVehicleId();
	}

	@Override
	public double getActivityEndTime() {
		return delegate.getActivityEndTime();
	}

	@Override
	public void endActivityAndComputeNextState(final double now) {
		delegate.endActivityAndComputeNextState(now);
	}

	@Override
	public void setStateToAbort(final double now) {
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
	public String getMode() {
		return delegate.getMode();
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(final Id linkId) {
		delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return planDelegate.getCurrentPlanElement();
	}

	@Override
	public PlanElement getNextPlanElement() {
		return planDelegate.getNextPlanElement();
	}

	@Override
	public Plan getCurrentPlan() {
		return planDelegate.getCurrentPlan();
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// We need to call delegate, and not just check if next link is null
		// (as was done in the refactoring), because PersonDriverAgentImpl,
		// our usual delegate, does more than that.
		return delegate.isWantingToArriveOnCurrentLink();
	}

	@Override
	public String toString() {
		return "["+getClass().getSimpleName()+
			": id="+getId()+
			"; currentElement="+getCurrentPlanElement()+
			"; positionInPlan="+getCurrentPlan().getPlanElements().indexOf( getCurrentPlanElement() )+
			" / "+getCurrentPlan().getPlanElements().size()+
			"; currentLinkId="+getCurrentLinkId()+
			"; state="+getState()+
			"]";
	}

	@Override
	public boolean getEnterTransitRoute(
			final TransitLine line,
			final TransitRoute transitRoute,
			final List<TransitRouteStop> stopsToCome,
			final TransitVehicle transitVehicle) {
		if ( ptDelegate == null ) throw new UnsupportedOperationException( delegate.getClass().getName()+" do not provide PTPassengerAgent" );
		return ptDelegate.getEnterTransitRoute( line , transitRoute , stopsToCome , transitVehicle );
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		if ( ptDelegate == null ) throw new UnsupportedOperationException( delegate.getClass().getName()+" do not provide PTPassengerAgent" );
		return ptDelegate.getExitAtStop( stop );
	}

	@Override
	public Id getDesiredAccessStopId() {
		if ( ptDelegate == null ) throw new UnsupportedOperationException( delegate.getClass().getName()+" do not provide PTPassengerAgent" );
		return ptDelegate.getDesiredAccessStopId();
	}

	@Override
	public Id getDesiredDestinationStopId() {
		if ( ptDelegate == null ) throw new UnsupportedOperationException( delegate.getClass().getName()+" do not provide PTPassengerAgent" );
		return ptDelegate.getDesiredDestinationStopId();
	}

	@Override
	public double getWeight() {
		if ( ptDelegate == null ) throw new UnsupportedOperationException( delegate.getClass().getName()+" do not provide PTPassengerAgent" );
		return ptDelegate.getWeight();
	}

	@Override
	public Person getPerson() {
		return ((HasPerson) delegate).getPerson();
	}

	public PlanElement getPreviousPlanElement() {
		return this.planDelegate.getPreviousPlanElement();
	}

	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.delegate.getCurrentFacility();
	}

	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.delegate.getDestinationFacility();
	}
}
