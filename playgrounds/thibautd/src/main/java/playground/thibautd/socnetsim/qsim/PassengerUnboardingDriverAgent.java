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
package playground.thibautd.socnetsim.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.utils.IdentifiableCollectionsUtils;

class PassengerUnboardingDriverAgent implements MobsimDriverAgent, PlanAgent, PassengerAgent, PTPassengerAgent {
	private final MobsimDriverAgent delegate;
	private final PTPassengerAgent ptDelegate;
	private final PlanAgent planDelegate;
	private final QNetsimEngine netsimEngine;
	private final InternalInterface internalInterface;

	private final List<PassengerAgent> passengersToBoard = new ArrayList<PassengerAgent>();

	public PassengerUnboardingDriverAgent(
			final MobsimAgent delegate,
			final QNetsimEngine netsimEngine,
			final InternalInterface internalInterface) {
		this.delegate = (MobsimDriverAgent) delegate;
		this.ptDelegate = delegate instanceof PTPassengerAgent ? (PTPassengerAgent) delegate : null;
		this.planDelegate = (PlanAgent) delegate;
		this.netsimEngine = netsimEngine;
		this.internalInterface = internalInterface;
	}

	// /////////////////////////////////////////////////////////////////////////
	// modified methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void endLegAndComputeNextState(final double now) {
		if ( delegate.getMode().equals( JointActingTypes.DRIVER ) ) {
			if ( !passengersToBoard.isEmpty() ) {
				assert ((NetworkRoute) ((Leg) getCurrentPlanElement()).getRoute()).getLinkIds().isEmpty();
				boardPassengers();
				assert passengersToBoard.isEmpty();
			}

			final MobsimVehicle vehicle = netsimEngine.getVehicles().get( delegate.getPlannedVehicleId() );
			final Id linkId = delegate.getCurrentLinkId();
			final Collection<PassengerAgent> passengersToUnboard = new ArrayList<PassengerAgent>();
			assert vehicle != null;
			for ( PassengerAgent p : vehicle.getPassengers() ) {
				if ( p.getDestinationLinkId().equals( linkId ) ) {
					passengersToUnboard.add( p );
				}
			}

			final EventsManager events = internalInterface.getMobsim().getEventsManager();
			for (PassengerAgent p : passengersToUnboard) {
				vehicle.removePassenger( p );
				((MobsimAgent) p).notifyArrivalOnLinkByNonNetworkMode( delegate.getCurrentLinkId() );
				((MobsimAgent) p).endLegAndComputeNextState( now );
				events.processEvent(
						events.getFactory().createPersonLeavesVehicleEvent(
							now,
							p.getId(),
							vehicle.getId()));
				internalInterface.arrangeNextAgentState( (MobsimAgent) p );
			}
		}

		delegate.endLegAndComputeNextState( now );
	}

	@Override
	public void notifyMoveOverNode(final Id newLinkId) {
		if ( !passengersToBoard.isEmpty() ) {
			boardPassengers();
			assert passengersToBoard.isEmpty();
		}
		delegate.notifyMoveOverNode(newLinkId);
	}

	private void boardPassengers() {
		final MobsimVehicle vehicle = netsimEngine.getVehicles().get( delegate.getPlannedVehicleId() );
		final EventsManager events = internalInterface.getMobsim().getEventsManager();
		for ( PassengerAgent passenger : passengersToBoard ) {
			assert passenger.getCurrentLinkId().equals( getCurrentLinkId() ) : passenger+" is at "+passenger.getCurrentLinkId()+" instead of "+getCurrentLinkId()+" for driver "+this;
			assert ((Leg) getCurrentPlanElement()).getMode().equals( JointActingTypes.DRIVER ) : getCurrentPlanElement();
			assert ((DriverRoute) ((Leg) getCurrentPlanElement()).getRoute()).getPassengersIds().contains( passenger.getId() ) :
				passenger+" not in "+((DriverRoute) ((Leg) getCurrentPlanElement()).getRoute()).getPassengersIds()+" for driver "+this;
			vehicle.addPassenger( passenger );
			events.processEvent(
					events.getFactory().createPersonEntersVehicleEvent(
						internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
						passenger.getId(),
						vehicle.getId()));
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

	// /////////////////////////////////////////////////////////////////////////
	// pure delegation
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return delegate.getId();
	}

	@Override
	public void setVehicle(final MobsimVehicle veh) {
		delegate.setVehicle(veh);
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
	public void abort(final double now) {
		delegate.abort(now);
	}

	@Override
	public Double getExpectedTravelTime() {
		return delegate.getExpectedTravelTime();
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
	public Plan getSelectedPlan() {
		return planDelegate.getSelectedPlan();
	}

	@Override
	public String toString() {
		return "["+getClass().getSimpleName()+": id="+getId()+"; currentElement="+getCurrentPlanElement()+"; currentLinkId="+getCurrentLinkId()+"]";
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
}
