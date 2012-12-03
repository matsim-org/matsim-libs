/* *********************************************************************** *
 * project: org.matsim.*
 * JointTravelerAgent.java
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * A mobsim agent adding some information related to joint traveling
 * to the basic interface.
 *
 * @author thibautd
 */
public class JointTravelerAgent implements MobsimDriverAgent {
	private final MobsimDriverAgent delegate;
	private boolean anounceDriverMode = false;
	private Map<Id, List<Id>> passengersAtLink = null;

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public JointTravelerAgent(final MobsimDriverAgent delegate) {
		this.delegate = delegate;
	}

	public JointTravelerAgent(
			final Person person,
			final Plan plan,
			final Netsim netsim) {
		this.delegate = new PersonDriverAgentImpl( person , plan , netsim );
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * For agents which current plan element is a passenger leg, gives
	 * access to the id of the driver agent.
	 *
	 * @return the Id of the driver agent, or null if no driver
	 */
	public Id getDriverId() {
		try {
			Leg l = (Leg) getCurrentPlanElement();
			return ((PassengerRoute) l.getRoute()).getDriverId();
		}
		catch (Exception e) {
			throw new RuntimeException(
					"problem while getting driver id for person "+delegate.getId()
					+", current plan element "+getCurrentPlanElement(),
					e );
		}
	}

	/**
	 * For an agent with a forecoming driver leg. The agent accepts
	 * departure if and only if all its passengers are registered.
	 *
	 * @param time the time of arrival of the passenger at the link
	 * (currently unused, as we assume the calls are done in proper sequence.
	 * But should be used)
	 * @param passenger the id of the passenger
	 * @param linkId the id of the link the passenger is waiting at.
	 */
	public void notifyPassengerArrivedAtLink(
			final double time,
			final Id passenger,
			final Id linkId) {
		getPassengersAtLink( linkId ).add( passenger );
	}
	
	private List<Id> getPassengersAtLink(final Id link) {
		List<Id> ps = null;
		if (passengersAtLink == null) {
			passengersAtLink = new HashMap<Id, List<Id>>();
		}
		else {
			ps = passengersAtLink.get( link );
		}

		if (ps == null) {
			ps = new ArrayList<Id>();
			passengersAtLink.put( link , ps );
		}

		return ps;
	}

	/**
	 * Only meaningful for agents about to start a driver leg:
	 * it is false until every passenger was registered via
	 * {@link #notifyPassengerArrivedAtLink(Id,Id)} and the driver arrived at the link.
	 * @return true if the leg can start
	 */
	public boolean isReadyForDeparture(final Id linkId, final double time) {
		// cannot depart if not in "leg" mode (ie if still performing an activity
		// or if execution was aborted)
		if (getState() != MobsimAgent.State.LEG) return false;

		// now check if we are here or on another leg
		if ( !getCurrentLinkId().equals( linkId ) ) return false;

		// we are here, in a leg... check for passengers.
		List<Id> passengersAtLink = getPassengersAtLink( linkId );
		Collection<Id> ps = getPassengersIds();

		return ps.size() == passengersAtLink.size() && passengersAtLink.containsAll( ps );
	}

	public void notifyJointDeparture(final Id linkId) {
		passengersAtLink.remove( linkId );
	}

	public Collection<Id> getPassengersIds() {
		try {
			Leg l = (Leg) getCurrentPlanElement();
			Route r = l.getRoute();
			return r instanceof DriverRoute ? 
				((DriverRoute) r).getPassengersIds() :
				Collections.EMPTY_SET;
		}
		catch (Exception e) {
			throw new RuntimeException(
					"problem while getting passengers ids for person "+delegate.getId()
					+", current plan element "+getCurrentPlanElement(),
					e );
		}
	}

	@Override
	public String getMode() {
		return anounceDriverMode ? JointTripsEngine.DRIVER_SIM_MODE : delegate.getMode();
	}

	/**
	 * For the next leg, the agent will return the "simulated driver"
	 * mode when {@link #getMode()} is called.
	 * The idea is that the "non-simulated" (the one defined globally) mode
	 * is associated to the {@link JointTripsEngine}, and
	 * the simulated one to the "main mode" handler (vehicular) from the QSim.
	 * The {@link JointTripsEngine} handles waiting times, then
	 * calls this method, and asks the QSim to process this new state,
	 * resulting in moving our dear driver around properly, whith
	 * waiting time handling.
	 *
	 * @throws IllegalStateException if the current mode is not the (non-simulated)
	 * driver mode.
	 */
	public void anounceDriverMode() {
		if (getMode().equals( JointActingTypes.DRIVER )) {
			anounceDriverMode = true;
		}
		else {
			throw new IllegalStateException( "current mode is "+getMode() );
		}
	}

	@Override
	public void endLegAndComputeNextState(final double now) {
		anounceDriverMode = false;
		delegate.endLegAndComputeNextState(now);
	}

	private final PlanElement getCurrentPlanElement() {
		// it would be nice to do that without a cast
		// (nothing forces the delegate to be an instance
		// of this specific implementation)
		return ((PersonDriverAgentImpl) delegate).getCurrentPlanElement();
	}
	// /////////////////////////////////////////////////////////////////////////
	// delegate calls
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
	public Id chooseNextLinkId() {
		return delegate.chooseNextLinkId();
	}

	@Override
	public Id getDestinationLinkId() {
		return delegate.getDestinationLinkId();
	}

	@Override
	public void notifyMoveOverNode(final Id newLinkId) {
		delegate.notifyMoveOverNode(newLinkId);
	}

	@Override
	public void setVehicle(final MobsimVehicle veh) {
		delegate.setVehicle(veh);
	}

	@Override
	public MobsimVehicle getVehicle() {
		return delegate.getVehicle();
	}

	@Override
	public Id getPlannedVehicleId() {
		return delegate.getPlannedVehicleId();
	}

	@Override
	public State getState() {
		return delegate.getState();
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
	public void notifyArrivalOnLinkByNonNetworkMode(final Id linkId) {
		delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}
}

