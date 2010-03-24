/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.VehicleDepartsAtFacilityEventImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.PersonAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

public abstract class AbstractTransitDriver implements TransitDriverAgent, PassengerAccessEgress {

	private static final Logger log = Logger.getLogger(AbstractTransitDriver.class);

	private TransitVehicle vehicle = null;

	private int nextLinkIndex = 0;
	private final TransitStopAgentTracker agentTracker;
	private Person dummyPerson;
	private final QSim sim;
	private TransitRouteStop currentStop = null;
	private TransitRouteStop nextStop;
	private ListIterator<TransitRouteStop> stopIterator;

	public abstract void legEnds(final double now);
	public abstract NetworkRoute getCarRoute();
	public abstract TransitLine getTransitLine();
	public abstract TransitRoute getTransitRoute();
	public abstract Departure getDeparture();
	public abstract double getDepartureTime();

	public AbstractTransitDriver(QSim sim, TransitStopAgentTracker agentTracker2) {
		super();
		this.sim = sim;
		this.agentTracker = agentTracker2;
	}

	protected void init() {
		if (getTransitRoute() != null) {
			this.stopIterator = getTransitRoute().getStops().listIterator();
			this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		} else {
			this.nextStop = null;
		}
		this.nextLinkIndex = 0;
	}
	protected void setDriver(Person personImpl) {
		this.dummyPerson = personImpl;
	}

	@Override
	public Id chooseNextLinkId() {
		if (this.nextLinkIndex < getCarRoute().getLinkIds().size()) {
			return getCarRoute().getLinkIds().get(this.nextLinkIndex);
		}
		if (this.nextLinkIndex == getCarRoute().getLinkIds().size()) {
			return getCarRoute().getEndLinkId();
		}
		return null;
	}

	@Override
	public void moveOverNode() {
		this.nextLinkIndex++;
	}

	@Override
	public TransitStopFacility getNextTransitStop() {
		if (this.nextStop == null) {
			return null;
		}
		return this.nextStop.getStopFacility();
	}

	@Override
	public double handleTransitStop(final TransitStopFacility stop, final double now) {
		assertExpectedStop(stop);
		processEventVehicleArrives(stop, now);
		ArrayList<PassengerAgent> passengersLeaving = findPassengersLeaving(stop);
		int freeCapacity = this.vehicle.getPassengerCapacity() - this.vehicle.getPassengers().size() + passengersLeaving.size();
		List<PassengerAgent> passengersEntering = findPassengersEntering(stop, freeCapacity);
		double stopTime = this.vehicle.getStopHandler().handleTransitStop(stop, now, passengersLeaving, passengersEntering, this);
		if(stopTime == 0.0){
			stopTime = longerStopTimeIfWeAreAheadOfSchedule(now, stopTime);
		}
		if (stopTime == 0.0) {
			depart(now);
		}
		return stopTime;
	}

	@Override
	public void activityEnds(final double now) {
		this.sim.agentDeparts(now, this, this.getCurrentLeg().getRoute().getStartLinkId());
	}

	@Override
	public void teleportToLink(final Id linkId) {
	}

	QSim getSimulation(){
		return this.sim;
	}

	@Override
	public Person getPerson() {
		return this.dummyPerson;
	}

	public TransitVehicle getVehicle() {
		return this.vehicle;
	}

	public void setVehicle(final TransitVehicle vehicle) {
		this.vehicle = vehicle;
	}

	private void processEventVehicleArrives(final TransitStopFacility stop,
			final double now) {
		EventsManager events = this.sim.getEventsManager();
		if (this.currentStop == null) {
			this.currentStop = this.nextStop;
			events.processEvent(new VehicleArrivesAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), stop.getId(), now - this.getDeparture().getDepartureTime() - this.currentStop.getDepartureOffset()));
		}
	}

	private void assertExpectedStop(final TransitStopFacility stop) {
		if (stop != this.nextStop.getStopFacility()) {
			throw new RuntimeException("Expected different stop.");
		}
	}

	private double longerStopTimeIfWeAreAheadOfSchedule(final double now,
			final double stopTime) {
		if ((this.nextStop.isAwaitDepartureTime()) && (this.nextStop.getDepartureOffset() != Time.UNDEFINED_TIME)) {
			double earliestDepTime = getDepartureTime() + this.nextStop.getDepartureOffset();
			if (now + stopTime < earliestDepTime) {
				return earliestDepTime - now;
			}
		}
		return stopTime;
	}

	private void depart(final double now) {
		EventsManager events = this.sim.getEventsManager();
		events.processEvent(new VehicleDepartsAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), this.currentStop.getStopFacility().getId(), now - this.getDeparture().getDepartureTime() - this.currentStop.getDepartureOffset()));
		this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		if(this.nextStop == null) {
			assertVehicleIsEmpty();
		}
		this.currentStop = null;
	}

	private void assertVehicleIsEmpty() {
		if (this.vehicle.getPassengers().size() > 0) {
			RuntimeException e = new RuntimeException("Transit vehicle is at last stop but still contains passengers that did not leave the vehicle!");
			log.error("Transit vehicle must be empty after last stop! vehicle-id = " + this.vehicle.getBasicVehicle().getId(), e);
			for (PassengerAgent agent : this.vehicle.getPassengers()) {
				if (agent instanceof PersonAgent) {
				log.error("Agent is still in transit vehicle: agent-id = " + ((PersonAgent) agent).getPerson().getId());
				}
			}
			throw e;
		}
	}

	public void handlePassengerEntering(final PassengerAgent passenger, final double time) {
		this.agentTracker.removeAgentFromStop(passenger, this.currentStop.getStopFacility());
		this.vehicle.addPassenger(passenger);
		DriverAgent agent = (DriverAgent) passenger;
		EventsManager events = this.sim.getEventsManager();
		events.processEvent(new PersonEntersVehicleEventImpl(time, agent.getPerson().getId(), this.vehicle.getBasicVehicle(), this.getTransitRoute().getId()));
	}

	public void handlePassengerLeaving(final PassengerAgent passenger, final double time) {
		this.vehicle.removePassenger(passenger);
		DriverAgent agent = (DriverAgent) passenger;
		EventsManager events = this.sim.getEventsManager();
		events.processEvent(new PersonLeavesVehicleEventImpl(time, agent.getPerson().getId(), this.vehicle.getBasicVehicle().getId(), this.getTransitRoute().getId()));
		agent.teleportToLink(this.currentStop.getStopFacility().getLinkId());
//			events.processEvent(new AgentArrivalEventImpl(now, agent.getPerson(),
//					stop.getLink(), agent.getCurrentLeg()));
		agent.legEnds(time);
	}

	private List<PassengerAgent> findPassengersEntering(
			final TransitStopFacility stop, int freeCapacity) {
		ArrayList<PassengerAgent> passengersEntering = new ArrayList<PassengerAgent>();
		for (PassengerAgent agent : this.agentTracker.getAgentsAtStop(stop)) {
			if (freeCapacity == 0) {
				break;
			}
			List<TransitRouteStop> stops = getTransitRoute().getStops();
			List<TransitRouteStop> stopsToCome = stops.subList(stopIterator.nextIndex(), stops.size());
			if (agent.getEnterTransitRoute(getTransitLine(), getTransitRoute(), stopsToCome)) {
				passengersEntering.add(agent);
				freeCapacity--;
			}
		}
		return passengersEntering;
	}

	private ArrayList<PassengerAgent> findPassengersLeaving(
			final TransitStopFacility stop) {
		ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
		for (PassengerAgent passenger : this.vehicle.getPassengers()) {
			if (passenger.getExitAtStop(stop)) {
				passengersLeaving.add(passenger);
			}
		}
		return passengersLeaving;
	}

	protected NetworkRouteWrapper getWrappedCarRoute(NetworkRoute carRoute) {
		return new NetworkRouteWrapper(carRoute);
	}

	/**
	 * A simple wrapper that delegates all get-Methods to another instance, blocks set-methods
	 * so this will be read-only, and returns in getVehicleId() a vehicle-Id specific to this driver,
	 * and not to the NetworkRoute. This allows to share one NetworkRoute from a TransitRoute with
	 * multiple transit drivers, thus saving memory.
	 *
	 * @author mrieser
	 */
	protected class NetworkRouteWrapper implements NetworkRoute, Cloneable {

		private static final long serialVersionUID = 1L;
		private final NetworkRoute delegate;

		public NetworkRouteWrapper(final NetworkRoute route) {
			this.delegate = route;
		}

		@Override
		public List<Id> getLinkIds() {
			return this.delegate.getLinkIds();
		}

		@Override
		public NetworkRoute getSubRoute(final Id fromLinkId, final Id toLinkId) {
			return this.delegate.getSubRoute(fromLinkId, toLinkId);
		}

		@Override
		public double getTravelCost() {
			return this.delegate.getTravelCost();
		}

		@Override
		public Id getVehicleId() {
			return AbstractTransitDriver.this.vehicle.getBasicVehicle().getId();
		}

		@Override
		public void setLinkIds(final Id startLinkId, final List<Id> srcRoute, final Id endLinkId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setTravelCost(final double travelCost) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setVehicleId(final Id vehicleId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setEndLinkId(final Id  linkId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setStartLinkId(final Id linkId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public double getDistance() {
			return this.delegate.getDistance();
		}

		@Override
		public Id getEndLinkId() {
			return this.delegate.getEndLinkId();
		}

		@Override
		public Id getStartLinkId() {
			return this.delegate.getStartLinkId();
		}

		@Deprecated
		@Override
		public double getTravelTime() {
			return this.delegate.getTravelTime();
		}

		@Override
		public void setDistance(final double distance) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setTravelTime(final double travelTime) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public NetworkRouteWrapper clone() {
			try {
				return (NetworkRouteWrapper) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError(e);
			}
		}
	}


}