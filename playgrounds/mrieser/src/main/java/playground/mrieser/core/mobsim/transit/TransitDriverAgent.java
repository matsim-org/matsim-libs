/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.transit;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.VehicleDepartsAtFacilityEventImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.qsim.PassengerAccessEgress;
import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.api.NewSimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

public class TransitDriverAgent implements DriverAgent, PassengerAccessEgress {

	private final static Logger log = Logger.getLogger(TransitDriverAgent.class);

	private final PlanAgent agent;
	private final TransitMobsimVehicle vehicle;
	private final NewSimEngine simEngine;
	private Id currentLinkId = null;
	private final Id[] linkIds;
	private int nextLinkIndex = 0;
	private Id nextLinkId = null;
	private final TransitRouteStop[] stops;
	private TransitRouteStop nextStop;
	private TransitRouteStop currentStop = null;
	private int nextStopIndex = 0;
	private final TransitLine line;
	private final TransitRoute route;
	private final TransitFeature ptFeature;

	public TransitDriverAgent(final TransitDriverPlanAgent agent, final NewSimEngine simEngine, final NetworkRoute route, final TransitMobsimVehicle vehicle, final TransitFeature ptFeature) {
		this.agent = agent;
		this.simEngine = simEngine;
		this.vehicle = vehicle;
		this.ptFeature = ptFeature;

		UmlaufStueckI stueck = agent.getCurrentUmlaufStueck();
		this.line = stueck.getLine();
		this.route = stueck.getRoute();

		List<Id> tmpIds = route.getLinkIds();
		boolean sameEndAsStart = route.getStartLinkId().equals(route.getEndLinkId());
		boolean emptyRoute = tmpIds.size() == 0;

		if (sameEndAsStart && emptyRoute) {
			this.linkIds = new Id[2];
		} else {
			this.linkIds = new Id[3 + tmpIds.size()];
		}
		this.linkIds[0] = route.getStartLinkId();
		int index = 1;
		for (Id id : tmpIds) {
			this.linkIds[index] = id;
			index++;
		}
		if (sameEndAsStart && emptyRoute) {
			this.linkIds[index] = null; // sentinel
		} else {
			this.linkIds[index] = route.getEndLinkId();
			this.linkIds[index+1] = null; // sentinel
		}
		this.nextLinkId = this.linkIds[this.nextLinkIndex];
		List<TransitRouteStop> _stops = this.route.getStops();
		this.stops = _stops.toArray(new TransitRouteStop[_stops.size()]);
		this.nextStop = this.stops[nextStopIndex];
	}

	@Override
	public Id getNextLinkId() {
		return this.nextLinkId;
	}

	@Override
	public void notifyMoveToNextLink() {
		this.nextLinkIndex++;
		if (this.nextLinkIndex == this.linkIds.length) {
			this.nextLinkIndex--;
		}
		this.currentLinkId = this.nextLinkId;
		this.nextLinkId = this.linkIds[this.nextLinkIndex];
	}

	@Override
	public double getNextActionOnCurrentLink() {
		if (this.nextStop != null && this.nextStop.getStopFacility().getLinkId().equals(this.currentLinkId)) {
			return MobsimLink.POSITION_AT_TO_NODE;
		}
		if (this.nextLinkId == null) {
			return MobsimLink.POSITION_AT_TO_NODE;
		}
		return -1.0;
	}

	@Override
	public void handleNextAction(MobsimLink link, final double time) {
		if (this.nextStop != null && this.nextStop.getStopFacility().getLinkId().equals(this.currentLinkId)) {
			double delay = this.handleTransitStop(this.nextStop, time);
			if (delay > 0.0) {
				if (this.nextStop.getStopFacility().getIsBlockingLane()) {
					link.stopVehicle(this.vehicle);
				} else {
					link.removeVehicle(this.vehicle);
				}
				this.ptFeature.vehicleAtStop(this.vehicle, this.nextStop, time + delay, link, this);
			}
		} else {
			link.parkVehicle(this.vehicle);
			this.simEngine.handleAgent(this.agent);
		}
	}

	private void assertExpectedStop(final TransitStopFacility stop) {
		if (stop != this.nextStop.getStopFacility()) {
			throw new RuntimeException("Expected different stop.");
		}
	}

	private void processEventVehicleArrives(final TransitStopFacility stop, final double now) {
		EventsManager events = this.simEngine.getEventsManager();
		if (this.currentStop == null) {
			this.currentStop = this.nextStop;
			events.processEvent(new VehicleArrivesAtFacilityEventImpl(now, this.vehicle.getId(), stop.getId(), Double.NaN));
		}
	}

	private List<PassengerAgent> findPassengersEntering(final TransitStopFacility stop, final double freeCapacity) {
		double availCap = freeCapacity;
		ArrayList<PassengerAgent> passengersEntering = new ArrayList<PassengerAgent>();
		for (PassengerAgent agent : this.ptFeature.getAgentTracker().getAgentsAtStop(stop.getId())) {
			if (availCap < agent.getWeight()) {
				break;
			}
			List<TransitRouteStop> stops = this.route.getStops();
			List<TransitRouteStop> stopsToCome = stops.subList(this.nextStopIndex, stops.size());
			if (agent.getEnterTransitRoute(this.line, this.route, stopsToCome)) {
				passengersEntering.add(agent);
				availCap -= agent.getWeight();
			}
		}
		return passengersEntering;
	}

	private ArrayList<PassengerAgent> findPassengersLeaving(final TransitStopFacility stop) {
		ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
		for (PassengerAgent passenger : this.vehicle.getPassengers()) {
			if (passenger.getExitAtStop(stop)) {
				passengersLeaving.add(passenger);
			}
		}
		return passengersLeaving;
	}

	protected double longerStopTimeIfWeAreAheadOfSchedule(final double now, final double stopTime) {
		if ((this.nextStop.isAwaitDepartureTime()) && (this.nextStop.getDepartureOffset() != Time.UNDEFINED_TIME)) {
			double earliestDepTime = this.nextStop.getDepartureOffset();
			if (now + stopTime < earliestDepTime) {
				return earliestDepTime - now;
			}
		}
		return stopTime;
	}

	private void depart(final double now) {
		EventsManager events = this.simEngine.getEventsManager();
		events.processEvent(new VehicleDepartsAtFacilityEventImpl(now, this.vehicle.getId(),
				this.currentStop.getStopFacility().getId(), Double.NaN));
		if ((nextStopIndex + 1) < this.stops.length) {
			this.nextStopIndex++;
			this.nextStop = this.stops[this.nextStopIndex];
		} else {
			this.nextStop = null;
		}
		if (this.nextStop == null) {
			assertVehicleIsEmpty();
		}
		this.currentStop = null;
	}

	private void assertVehicleIsEmpty() {
		if (this.vehicle.getPassengers().size() > 0) {
			RuntimeException e = new RuntimeException("Transit vehicle is at last stop but still contains passengers that did not leave the vehicle!");
			log.error("Transit vehicle must be empty after last stop! vehicle-id = " + this.vehicle.getId(), e);
			throw e;
		}
	}

	private double handleTransitStop(TransitRouteStop stop, double time) {
		TransitStopFacility stopFac = stop.getStopFacility();
		assertExpectedStop(stopFac);
		processEventVehicleArrives(stopFac, time);
		ArrayList<PassengerAgent> passengersLeaving = findPassengersLeaving(stopFac);
		double freeCapacity = this.vehicle.getFreeCapacity();
		for (PassengerAgent a : passengersLeaving) {
			freeCapacity += a.getWeight();
		}
		List<PassengerAgent> passengersEntering = findPassengersEntering(stopFac, freeCapacity);
		double stopTime = this.vehicle.getStopHandler().handleTransitStop(stopFac, time, passengersLeaving, passengersEntering, this);
		if(stopTime == 0.0){
			stopTime = longerStopTimeIfWeAreAheadOfSchedule(time, stopTime);
		}
		if (stopTime == 0.0) {
			depart(time);
		}
		return stopTime;
	}

	@Override
	public boolean handlePassengerEntering(PassengerAgent agent, double time) {
		boolean handled = this.vehicle.addPassenger(agent);
		if (handled) {
			this.ptFeature.getAgentTracker().removeAgentFromStop(agent, this.currentStop.getStopFacility().getId());
			EventsManager events = this.simEngine.getEventsManager();
			events.processEvent(((EventsFactoryImpl) events.getFactory()).createPersonEntersVehicleEvent(time,
					agent.getId(), this.vehicle.getId(), this.route.getId()));
		}
		return handled;
	}

	@Override
	public boolean handlePassengerLeaving(PassengerAgent agent, double time) {
		boolean handled = this.vehicle.removePassenger(agent);
		if (handled) {
			EventsManager events = this.simEngine.getEventsManager();
			events.processEvent(new PersonLeavesVehicleEventImpl(time, agent.getId(), this.vehicle.getId(), this.route.getId()));
//			agent.notifyTeleportToLink(this.currentStop.getStopFacility().getLinkId());
//			agent.endLegAndAssumeControl(time);
		}
		return handled;
	}

}
