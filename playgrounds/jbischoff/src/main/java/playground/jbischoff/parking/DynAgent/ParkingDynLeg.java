/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.jbischoff.parking.DynAgent;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import playground.jbischoff.parking.choice.ParkingChoiceLogic;
import playground.jbischoff.parking.events.StartParkingSearchEvent;
import playground.jbischoff.parking.manager.ParkingManager;

public class ParkingDynLeg implements DriverDynLeg {
	private final NetworkRoute route;
	private int currentLinkIdx;
	private final String mode;
	private Tuple<Id<Link>, Id<Link>> currentAndNextParkLink = null;
	Id<Link> currentLinkId;
	private boolean parkingMode = false;
	ParkingManager parkingManager;
	Id<Vehicle> vehicleId;
	private ParkingChoiceLogic logic;
	private MobsimTimer timer;
	private EventsManager events;
	private boolean hasFoundParking = false;

	public ParkingDynLeg(String mode, NetworkRoute route, ParkingChoiceLogic logic, ParkingManager parkingManager,
			Id<Vehicle> vehicleId, MobsimTimer timer, EventsManager events) {
		this.mode = mode;
		this.route = route;
		currentLinkIdx = -1;
		this.currentLinkId = route.getStartLinkId();
		this.logic = logic;
		this.parkingManager = parkingManager;
		this.vehicleId = vehicleId;
		this.timer = timer;
		this.events=events;
	}

	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkIdx++;
		currentLinkId = newLinkId;
		if (!parkingMode) {
			if (currentLinkId.equals(this.getDestinationLinkId())) {
				this.parkingMode = true;
				this.events.processEvent(new StartParkingSearchEvent(timer.getTimeOfDay(), vehicleId, currentLinkId));
			}
		} else {
			hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, currentLinkId);

		}

	}

	@Override
	public Id<Link> getNextLinkId() {
		if (!parkingMode) {
			List<Id<Link>> linkIds = route.getLinkIds();

			if (currentLinkIdx == linkIds.size() - 1) {
				return route.getEndLinkId();
			}

			return linkIds.get(currentLinkIdx + 1);

		} else {
			if (hasFoundParking) {
				// easy, we can just park where at our destination link
				return null;
			} else {
				if (this.currentAndNextParkLink != null) {
					if (currentAndNextParkLink.getFirst().equals(currentLinkId)) {
						// we already calculated this
						return currentAndNextParkLink.getSecond();
					}
				}
				// need to find the next link
				Id<Link> nextLinkId = this.logic.getNextLink(currentLinkId);
				currentAndNextParkLink = new Tuple<Id<Link>, Id<Link>>(currentLinkId, nextLinkId);
				return nextLinkId;

			}
		}
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		// used only for teleportation
		return route.getEndLinkId();
	}

	@Override
	public void finalizeAction(double now) {
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
		if (!getDestinationLinkId().equals(linkId)) {
			throw new IllegalStateException();
		}

		currentLinkIdx = route.getLinkIds().size();
	}

	@Override
	public Double getExpectedTravelTime() {
		// travel time estimation does not take into account time required for
		// parking search
		// TODO add travel time at the destination link??
		return route.getTravelTime();
	}

	public Double getExpectedTravelDistance() {
		// travel time estimation does not take into account the distance
		// required for parking search
		// TODO add length of the destination link??
		return route.getDistance();
	}
}
