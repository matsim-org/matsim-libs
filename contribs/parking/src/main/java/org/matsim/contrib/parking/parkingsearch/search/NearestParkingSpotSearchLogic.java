/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package org.matsim.contrib.parking.parkingsearch.search;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * @author Ricardo Ewert
 */

public class NearestParkingSpotSearchLogic implements ParkingSearchLogic {

	private final Network network;
	private final Map<Id<ActivityFacility>, ActivityFacility> activityFacilities;
	private final ParkingRouter parkingRouter;
	private final ParkingSearchManager parkingManager;
	private NetworkRoute actualRoute = null;
	private final boolean canReserveParkingSlot;
	private final boolean canCheckParkingCapacitiesInAdvanced;
	private double distanceFromBaseToAttraction = Double.NaN;
	private boolean useRandomLinkChoice;
	private int currentLinkIdx;
	private final HashSet<Id<ActivityFacility>> triedParking;
	private Id<Link> nextLink;
	private boolean skipParkingActivity = false;
	private static final SplittableRandom random = new SplittableRandom(4711);

	/**
	 * {@link Network} the network
	 *
	 * @param parkingManager
	 */
	public NearestParkingSpotSearchLogic(Network network, ParkingRouter parkingRouter, ParkingSearchManager parkingManager,
										 boolean canReserveParkingSlot, boolean canCheckParkingCapacitiesInAdvanced) {
		this.network = network;
		this.parkingRouter = parkingRouter;
		this.parkingManager = parkingManager;
		this.canReserveParkingSlot = canReserveParkingSlot;
		this.canCheckParkingCapacitiesInAdvanced = canCheckParkingCapacitiesInAdvanced;
		activityFacilities = ((FacilityBasedParkingManager) parkingManager).getParkingFacilitiesById();
		currentLinkIdx = 0;
		triedParking = new HashSet<>();
		nextLink = null;
		useRandomLinkChoice = false;
	}

	/**
	 * @param baseLinkId linkId of the origin destination where the parkingSearch starts
	 */
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Link> baseLinkId, Id<Vehicle> vehicleId, String mode, double now,
								double maxParkingDuration, double nextPickupTime, boolean passangerInteractionAtParkingFacilityAtEndOfLeg,
								Coord coordOfAttraction) {
		double maxAdditionalDistanceToAttraction = Double.MAX_VALUE;
		// if a passenger interaction take place at a facility the walking distance to the attraction should be not extended by the value of
		// maxAdditionalDistanceToAttraction
		if (passangerInteractionAtParkingFacilityAtEndOfLeg) {
			if (Double.isNaN(distanceFromBaseToAttraction)) {
				findDistanceBetweenBaseLinkAndAttraction(baseLinkId, coordOfAttraction);
			}
			maxAdditionalDistanceToAttraction = 200.;
		}
		if (actualRoute == null) {
			actualRoute = findRouteToNearestParkingFacility(baseLinkId, currentLinkId, canCheckParkingCapacitiesInAdvanced, now,
				maxParkingDuration,
				passangerInteractionAtParkingFacilityAtEndOfLeg, maxAdditionalDistanceToAttraction, coordOfAttraction);
			if (!passangerInteractionAtParkingFacilityAtEndOfLeg) {
				checkIfDrivingToNextParkingLocationIsPossible(currentLinkId, baseLinkId, now, nextPickupTime);
			}
			triedParking.clear();
		} else if (currentLinkId.equals(actualRoute.getEndLinkId()) && !skipParkingActivity) {
			currentLinkIdx = 0;
			actualRoute = findRouteToNearestParkingFacility(baseLinkId, currentLinkId, canCheckParkingCapacitiesInAdvanced, now,
				maxParkingDuration,
				passangerInteractionAtParkingFacilityAtEndOfLeg, maxAdditionalDistanceToAttraction, coordOfAttraction);
			if (!passangerInteractionAtParkingFacilityAtEndOfLeg) {
				checkIfDrivingToNextParkingLocationIsPossible(currentLinkId, baseLinkId, now, nextPickupTime);
			}
		}
		if (actualRoute != null) {
			actualRoute.setVehicleId(vehicleId);
		}
		//if no possible parking was found. The vehicle takes a random next link. Background assumption: parking only at given parking slots
		if (actualRoute == null) {
			List<Link> outGoingLinks = ParkingUtils.getOutgoingLinksForMode(network.getLinks().get(currentLinkId), mode);
			return outGoingLinks.get(random.nextInt(outGoingLinks.size())).getId();
		}
		if (currentLinkIdx == actualRoute.getLinkIds().size()) {
			return actualRoute.getEndLinkId();
		}
		nextLink = actualRoute.getLinkIds().get(currentLinkIdx);
		currentLinkIdx++;

		return nextLink;

	}

	/**
	 * Calculates the EuclideanDistance between the planed parkingLocation to the related attraction.
	 *
	 * @param baseLinkId
	 * @param coordOfAttraction
	 */
	private void findDistanceBetweenBaseLinkAndAttraction(Id<Link> baseLinkId, Coord coordOfAttraction) {
		for (ActivityFacility activityFacility : activityFacilities.values()) {
			if (activityFacility.getLinkId().equals(baseLinkId)) {
				distanceFromBaseToAttraction = NetworkUtils.getEuclideanDistance(activityFacility.getCoord(),
					coordOfAttraction);
			}
		}
	}

	/**
	 * Checks if it is possible to drive to the new parking facility and to drive back to the base without extending the startTime of the following
	 * activity.
	 * If the resulting parking time at the new facility is less then 5 minutes the vehicle will drive directly to the next activity location.
	 *
	 * @param currentLinkId
	 * @param baseLinkId
	 * @param now
	 * @param nextPickupTime
	 */
	private void checkIfDrivingToNextParkingLocationIsPossible(Id<Link> currentLinkId, Id<Link> baseLinkId, double now, double nextPickupTime) {
		double expectedTravelTimeFromParkingToBase;

		if (actualRoute == null) {
			expectedTravelTimeFromParkingToBase = this.parkingRouter.getRouteFromParkingToDestination(baseLinkId, now,
				currentLinkId).getTravelTime().seconds(); //TODO better: use the nextLink for the check
		} else {
			expectedTravelTimeFromParkingToBase = this.parkingRouter.getRouteFromParkingToDestination(baseLinkId, now,
				actualRoute.getEndLinkId()).getTravelTime().seconds();
		}
		double minimumExpectedParkingDuration = 5 * 60;
		double travelTimeNextPart;
		if (actualRoute == null) {
			travelTimeNextPart = 0.;
		} else {
			travelTimeNextPart = actualRoute.getTravelTime().seconds();
		}

		if ((nextPickupTime - now - travelTimeNextPart - expectedTravelTimeFromParkingToBase) < minimumExpectedParkingDuration) {
			actualRoute = this.parkingRouter.getRouteFromParkingToDestination(baseLinkId, now,
				currentLinkId);
			skipParkingActivity = true;
		}
	}

	public Id<Link> getNextParkingLocation() {
		if (actualRoute == null) {
			return null;
		}
		return actualRoute.getEndLinkId();
	}

	/**
	 * If the next parking activity is skipped because the given constraints are not fulfilled, it returns true.
	 *
	 * @return
	 */
	public boolean isNextParkingActivitySkipped() {
		return skipParkingActivity;
	}

	public NetworkRoute getNextRoute() {
		return actualRoute;
	}

	public boolean canReserveParkingSlot() {
		return canReserveParkingSlot;
	}

	/**
	 * If no possible parking was found the vehicle selects a random outgoing link.
	 *
	 * @return
	 */
	public boolean isUseRandomLinkChoice() {
		return useRandomLinkChoice;
	}

	private NetworkRoute findRouteToNearestParkingFacility(Id<Link> baseLinkId, Id<Link> currentLinkId, boolean canCheckParkingCapacitiesInAdvanced,
														   double now, double maxParkingDuration,
														   boolean passangerInteractionAtParkingFacilityAtEndOfLeg, double maxDistanceFromBase,
														   Coord coordOfAttraction) {
		TreeMap<Double, ActivityFacility> euclideanDistanceToParkingFacilities = new TreeMap<>();
		ActivityFacility nearstActivityFacility = null;
		NetworkRoute selectedRoute = null;
		double minTravelTime = Double.MAX_VALUE;
		for (ActivityFacility activityFacility : activityFacilities.values()) {
			if (triedParking.size() == activityFacilities.size()) {
				triedParking.clear();
			}
			if (!passangerInteractionAtParkingFacilityAtEndOfLeg && triedParking.contains(activityFacility.getId())) {
				continue;
			}
			if (canCheckParkingCapacitiesInAdvanced) {
				if (((FacilityBasedParkingManager) parkingManager).getNrOfFreeParkingSpacesOnLink(activityFacility.getLinkId()) < 1) {
					continue;
				}
			}
			double latestEndOfParking = now + maxParkingDuration;
			// checks if parking slot is now open and still open when the parking will finish; if no openingTime is set, we assume that it is open
			// the hole day
			ActivityOption parkingOptions = activityFacility.getActivityOptions().get("parking");
			if (!parkingOptions.getOpeningTimes().isEmpty()) {
				if (parkingOptions.getOpeningTimes().first().getStartTime() > now && parkingOptions.getOpeningTimes().first()
																								   .getEndTime() < latestEndOfParking) {
					continue;
				}
			}
			//check if approx. the max parking time at facility will not exceed, assumption: "parking duration - 30 minutes" is parking Time.
			if (activityFacility.getAttributes().getAsMap().containsKey("maxParkingDurationInHours")) { //TODO vielleicht etwas sparsamer machen
				double maxParkingDurationAtFacility = 3600 * (double) activityFacility.getAttributes().getAsMap().get("maxParkingDurationInHours");
				if (maxParkingDuration - 30 * 60 > maxParkingDurationAtFacility) {
					continue;
				}
			}

			//TODO beschreiben was passiert
			if (passangerInteractionAtParkingFacilityAtEndOfLeg) {
				double distanceBetweenThisParkingFacilityAndTheAttraction = NetworkUtils.getEuclideanDistance(activityFacility.getCoord(),
					coordOfAttraction);
				if (distanceBetweenThisParkingFacilityAndTheAttraction - distanceFromBaseToAttraction > maxDistanceFromBase) {
					continue;
				}
			}
			// create Euclidean distances to the parking activities to find routes only to the nearest facilities in the next step
			Coord coordBaseLink = network.getLinks().get(baseLinkId).getCoord();
			Coord coordCurrentLink = network.getLinks().get(currentLinkId).getCoord();

			double distanceBaseAndFacility = NetworkUtils.getEuclideanDistance(activityFacility.getCoord(), coordBaseLink);
			double distanceCurrentAndFacility;
			if (passangerInteractionAtParkingFacilityAtEndOfLeg) {
				distanceCurrentAndFacility = 0.;
			} else {
				distanceCurrentAndFacility = NetworkUtils.getEuclideanDistance(activityFacility.getCoord(), coordCurrentLink);
			}

			double distanceForParking = distanceBaseAndFacility + distanceCurrentAndFacility;
			euclideanDistanceToParkingFacilities.put(distanceForParking, activityFacility);
		}
		int counter = 0;
		int numberOfCheckedRoutes = 5;

		// selects the parking facility with the minimum travel time; only investigates the nearest facilities
		for (ActivityFacility activityFacility : euclideanDistanceToParkingFacilities.values()) {
			if (activityFacility.getAttributes().getAsMap().containsKey("maxParkingDurationInHours")) {
				double maxParkingDurationAtFacility = 3600 * (double) activityFacility.getAttributes().getAsMap().get("maxParkingDurationInHours");
				double expectedTravelTimeFromParkingToBase = getExpectedTravelTime(baseLinkId, now, activityFacility.getLinkId());
				double expectedTravelTimeFromCurrentToParking = getExpectedTravelTime(activityFacility.getLinkId(), now, currentLinkId);
				double expectedParkingTime = maxParkingDuration - expectedTravelTimeFromCurrentToParking - expectedTravelTimeFromParkingToBase;
				if (expectedParkingTime > maxParkingDurationAtFacility) {
					continue;
				}
			}
			counter++;
			if (passangerInteractionAtParkingFacilityAtEndOfLeg && euclideanDistanceToParkingFacilities.size() > triedParking.size()) {
				if (triedParking.contains(activityFacility.getId())) {
					continue;
				}
			}
			if (passangerInteractionAtParkingFacilityAtEndOfLeg && euclideanDistanceToParkingFacilities.size() == triedParking.size()) {
				triedParking.clear();
			}
			NetworkRoute possibleRoute = this.parkingRouter.getRouteFromParkingToDestination(activityFacility.getLinkId(), now,
				currentLinkId);
			// reason is that we expect that the driver will always take the nearest possible getOff point to reduce the walk distance for the guests
			if (passangerInteractionAtParkingFacilityAtEndOfLeg) {
				selectedRoute = possibleRoute;
				nearstActivityFacility = activityFacility;
				break;
			}
			double travelTimeToParking = possibleRoute.getTravelTime().seconds();
			double travelTimeFromParking = travelTimeToParking;
			if (!baseLinkId.equals(currentLinkId)) {
				NetworkRoute routeFromParkingToBase = this.parkingRouter.getRouteFromParkingToDestination(baseLinkId, now,
					activityFacility.getLinkId());
				travelTimeFromParking = routeFromParkingToBase.getTravelTime().seconds();
			}

			double calculatedTravelTime = travelTimeToParking + travelTimeFromParking;
			if (calculatedTravelTime < minTravelTime) {
				selectedRoute = possibleRoute;
				minTravelTime = calculatedTravelTime;
				nearstActivityFacility = activityFacility;
			}
			if (counter == numberOfCheckedRoutes) {
				break;
			}
		}

		if (selectedRoute == null) {
			useRandomLinkChoice = true;
			return null;
		}
		triedParking.add(nearstActivityFacility.getId());
		actualRoute = selectedRoute;
		return actualRoute;
	}

	/**
	 * @param destinationLinkId
	 * @param now
	 * @param currentLinkId
	 * @return
	 */
	public double getExpectedTravelTime(Id<Link> destinationLinkId, double now,
										Id<Link> currentLinkId) {
		NetworkRoute possibleRoute = this.parkingRouter.getRouteFromParkingToDestination(destinationLinkId, now,
			currentLinkId);
		return possibleRoute.getTravelTime().seconds();
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId, String mode) {
		throw new RuntimeException("shouldn't happen - method not implemented");
	}

	@Override
	public void reset() {
		actualRoute = null;
		currentLinkIdx = 0;
		skipParkingActivity = false;
		distanceFromBaseToAttraction = Double.NaN;
	}

}
