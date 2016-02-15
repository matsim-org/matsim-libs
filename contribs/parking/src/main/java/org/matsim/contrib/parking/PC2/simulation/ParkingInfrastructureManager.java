/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.PC2.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.PC2.infrastructure.PrivateParking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.infrastructure.RentableParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;

// TODO: make abstract and create algorithm in Zuerich case -> provide protected helper methods already here.
public final class ParkingInfrastructureManager {

	private ParkingScoreManager parkingScoreManager;
	private HashMap<Id<PC2Parking>, PC2Parking> allParkings;
	private HashMap<Id<Person>, Id<PC2Parking>> parkedVehicles;
	private EventsManager eventsManager;

	// facilityId -> parkings available to users of that facility
	private LinkedListValueHashMap<Id<ActivityFacility>, PPRestrictedToFacilities> privateParkingsRestrictedToFacilities;

	// available to parking owner and sometimes to others (renting out)
	private HashMap<Id<Person>, RentableParking> rentablePrivateParking;
	// TODO: cont here!!!!

	// owner can always use it -> also for other activities (e.g. shopping close
	// to home)

	// => public or rentable parking useable.

	// TODO: later - to improve parformance, a second variable could be added,
	// where full parking are put.
	private QuadTree<PC2Parking> publicParkingsQuadTree;

	private HashMap<String, QuadTree<PC2Parking>> publicParkingGroupQuadTrees;

	// TODO: make private parking (attached to facility)
	// + also private parking, which is attached to activity
	// both should be checked.

	public ParkingInfrastructureManager(ParkingScoreManager parkingScoreManager, EventsManager eventsManager) {
		this.parkingScoreManager = parkingScoreManager;
		this.eventsManager = eventsManager;
		parkedVehicles = new HashMap<>();
		setAllParkings(new HashMap<Id<PC2Parking>, PC2Parking>());
		privateParkingsRestrictedToFacilities = new LinkedListValueHashMap<>();
		rentablePrivateParking = new HashMap<>();
	}

	public synchronized void setPublicParkings(LinkedList<PublicParking> publicParkings) {
		publicParkingGroupQuadTrees = new HashMap<String, QuadTree<PC2Parking>>();
		EnclosingRectangle allPublicParkingRect = new EnclosingRectangle();
		HashMap<String, EnclosingRectangle> groupRects = new HashMap<String, EnclosingRectangle>();

		for (PublicParking parking : publicParkings) {
			if (parking.getAvailableParkingCapacity() <= 0) {
				DebugLib.stopSystemAndReportInconsistency("parking capacity non-positive: " + parking.getId());
			}

			allPublicParkingRect.registerCoord(parking.getCoordinate());
			getAllParkings().put(parking.getId(), parking);

			if (!groupRects.containsKey(parking.getGroupName())) {
				groupRects.put(parking.getGroupName(), new EnclosingRectangle());
			}
			EnclosingRectangle groupRect = groupRects.get(parking.getGroupName());
			groupRect.registerCoord(parking.getCoordinate());
		}
		this.publicParkingsQuadTree = (new QuadTreeInitializer<PC2Parking>()).getQuadTree(allPublicParkingRect);

		for (String groupNames : groupRects.keySet()) {
			publicParkingGroupQuadTrees.put(groupNames,
					(new QuadTreeInitializer<PC2Parking>()).getQuadTree(groupRects.get(groupNames)));
		}

		for (PublicParking parking : publicParkings) {
			addParkingToQuadTree(publicParkingsQuadTree, parking);
			addParkingToQuadTree(publicParkingGroupQuadTrees.get(parking.getGroupName()), parking);
		}
	}

	public static void addParkingToQuadTree(QuadTree<PC2Parking> quadTree, PC2Parking parking) {
		quadTree.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), parking);
	}

	public synchronized void setRentableParking(LinkedList<RentableParking> rentableParkings) {
		for (RentableParking pp : rentableParkings) {
			rentablePrivateParking.put(pp.getOwnerId(), pp);
			getAllParkings().put(pp.getId(), pp);
		}
	}

	public synchronized void setPrivateParkingRestrictedToFacilities(
			LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities) {
		for (PPRestrictedToFacilities pp : ppRestrictedToFacilities) {
			for (Id<ActivityFacility> facilityId : pp.getFacilityIds()) {
				privateParkingsRestrictedToFacilities.put(facilityId, pp);
				getAllParkings().put(pp.getId(), pp);
			}
		}
	}

	public synchronized void reset() {
		parkedVehicles.clear();

		for (PC2Parking parking : getAllParkings().values()) {
			if (parking.getAvailableParkingCapacity() == 0) {
				// yyyyyy I have no idea what this is doing and why. Why should
				// parking be full at the end of simulation? Why put it
				// into a quad tree? Maybe this is an implicit marker that has
				// been added during the previous iteration??? kai, jul'15
				if (!(parking instanceof PrivateParking)) {
					addParkingToQuadTree(publicParkingsQuadTree, parking);
					addParkingToQuadTree(publicParkingGroupQuadTrees.get(parking.getGroupName()), parking);
					// yyyyyy I could speculate that full parking lots are
					// removed from the quad tree, and it is re-added as soon
					// as space becomes available. But this is not commented,
					// and it also does not look like it would work in that way.
					// kai, jul'15
				}
			}
			parking.resetAvailability();
			// (resets the available parking capacity to the maximum parking
			// capacity, which should be > 0. kai, jul15)

			if (parking.getAvailableParkingCapacity() == 0) {
				DebugLib.stopSystemAndReportInconsistency();
			}
		}

		// availablePublicParkingAtCityCentre();
	}

	private synchronized QuadTree<PC2Parking> getPublicParkingQuadTree() {
		return publicParkingsQuadTree;
	}

	public synchronized PC2Parking parkAtClosestPublicParkingNonPersonalVehicle(Coord destCoordinate,
			String groupName) {
		PC2Parking parking = null;
		if (groupName == null) {
			parking = publicParkingsQuadTree.getClosest(destCoordinate.getX(), destCoordinate.getY());
		} else {
			QuadTree<PC2Parking> quadTree = publicParkingGroupQuadTrees.get(groupName);
			parking = quadTree.getClosest(destCoordinate.getX(), destCoordinate.getY());

			if (parking == null) {
				DebugLib.stopSystemAndReportInconsistency(
						"not enough parking available for parkingGroupName:" + groupName);
			}
		}
		parkVehicle(parking);

		return parking;
	}

	public synchronized void logArrivalEventAtTimeZero(PC2Parking parking) {
		eventsManager.processEvent(new ParkingArrivalEvent(0, parking.getId(), null, null, 0));
	}

	public synchronized PC2Parking parkAtClosestPublicParkingNonPersonalVehicle(Coord destCoordinate, String groupName,
			Id<Person> personId, double parkingDurationInSeconds, double arrivalTime) {
		PC2Parking parking = parkAtClosestPublicParkingNonPersonalVehicle(destCoordinate, groupName);

		double walkScore = parkingScoreManager.calcWalkScore(destCoordinate, parking, personId,
				parkingDurationInSeconds);
		parkingScoreManager.addScore(personId, walkScore);

		eventsManager.processEvent(
				new ParkingArrivalEvent(arrivalTime, parking.getId(), personId, destCoordinate, walkScore));

		return parking;
	}

	// TODO: make this method abstract
	// when person/vehicleId is clearly distinct, then I can change this to
	// vehicleId - check, if this is the case now.
	public synchronized PC2Parking parkVehicle(ParkingOperationRequestAttributes parkingOperationRequestAttributes) {
		// availablePublicParkingAtCityCentre();

		double finalScore = 0;

		PC2Parking selectedParking = null;
		boolean parkingFound = false;

		// first search for parking at selected facility:
		for (PPRestrictedToFacilities pp : privateParkingsRestrictedToFacilities
				.get(parkingOperationRequestAttributes.facilityId)) {
			if (pp.getAvailableParkingCapacity() > 0) {

				// this tells the parking lot to decrease the number of
				// available spaces:
				pp.parkVehicle();

				// this puts the personId (!!!! yyyyyy) at the parking location:
				parkedVehicles.put(parkingOperationRequestAttributes.personId, pp.getId());

				parkingFound = true;
				selectedParking = pp;
			}
		}

		// if not found, search within distance of 300m:
		double distance = 300;
		if (!parkingFound) {
			Collection<PC2Parking> collection = getFilteredCollection(parkingOperationRequestAttributes, distance);

			while (collection.size() == 0) {
				distance *= 2;
				collection = getFilteredCollection(parkingOperationRequestAttributes, distance);

				if (distance > 100000000) {
					// stop infinite loop
					DebugLib.stopSystemAndReportInconsistency(
							"not enough public parking in scenario - introduce dummy parking to solve problem");
				}

			}

			// put parking that was found into a sorted queue:
			PriorityQueue<SortableMapObject<PC2Parking>> queue = new PriorityQueue<SortableMapObject<PC2Parking>>();
			for (PC2Parking parking : collection) {
				double score = parkingScoreManager.calcScore(parkingOperationRequestAttributes.destCoordinate,
						parkingOperationRequestAttributes.arrivalTime,
						parkingOperationRequestAttributes.parkingDurationInSeconds, parking,
						parkingOperationRequestAttributes.personId, parkingOperationRequestAttributes.legIndex, false);
				queue.add(new SortableMapObject<PC2Parking>(parking, -1.0 * score)); // score made positive, so that priority queue works properly
			}

			// TODO: should I make MNL only on top 5 here?

			// select the best one from the queue:
			SortableMapObject<PC2Parking> poll = queue.peek();
			finalScore = poll.getScore();
			selectedParking = poll.getKey();

			if (selectedParking instanceof RentableParking) {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}

			if (rentablePrivateParking.containsKey(parkingOperationRequestAttributes.personId)) {
				RentableParking rentableParking = rentablePrivateParking
						.get(parkingOperationRequestAttributes.personId);

				if (rentableParking.getAvailableParkingCapacity() > 0) {

					double score = parkingScoreManager.calcScore(parkingOperationRequestAttributes.destCoordinate,
							parkingOperationRequestAttributes.arrivalTime,
							parkingOperationRequestAttributes.parkingDurationInSeconds, rentableParking,
							parkingOperationRequestAttributes.personId, parkingOperationRequestAttributes.legIndex,
							true);

					if (score > finalScore*-1) {
						selectedParking = rentableParking;
					}
				}
			}

			if (finalScore>0){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			
			
			if (selectedParking instanceof RentableParking) {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}

			// this puts the personId (!!!! yyyyyy) at the parking location:
			parkedVehicles.put(parkingOperationRequestAttributes.personId, selectedParking.getId());

			// this tells the parking lot to decrease the number of
			// available spaces:
			parkVehicle(selectedParking);

			// PC2Parking closestParking =
			// getPublicParkingQuadTree().get(parkingOperationRequestAttributes.destCoordinate.getX(),
			// parkingOperationRequestAttributes.destCoordinate.getY());
			// double distanceClosestParking =
			// GeneralLib.getDistance(closestParking.getCoordinate(),
			// parkingOperationRequestAttributes.destCoordinate);

			// double distanceSelectedParking =
			// GeneralLib.getDistance(selectedParking.getCoordinate(),
			// parkingOperationRequestAttributes.destCoordinate);
			//
			// if (selectedParking.getId().toString().contains("stp") &&
			// distanceSelectedParking>300) {
			// DebugLib.emptyFunctionForSettingBreakPoint();
			// }
			//
			// if (distanceSelectedParking > distanceClosestParking * 1.5 &&
			// distanceSelectedParking > 200) {
			// Id closestParkingId = closestParking.getId();
			// Id selectedParkingId = selectedParking.getId();
			//
			// if (closestParkingId.toString().contains("stp") &&
			// selectedParkingId.toString().contains("stp")) {
			//
			// for (Parking parking : collection) {
			// if (parking.getId().toString().contains("stp")) {
			// System.out.println(parking.getId()
			// + "\t"
			// + Math.round(GeneralLib.getDistance(parking.getCoordinate(),
			// parkingOperationRequestAttributes.destCoordinate)));
			// }
			// }
			//
			// while (queue.size() > 0) {
			// SortableMapObject<Parking> p = queue.poll();
			// double costScore =
			// parkingScoreManager.calcCostScore(parkingOperationRequestAttributes.arrivalTime,
			// parkingOperationRequestAttributes.parkingDurationInSeconds,
			// p.getKey(),
			// parkingOperationRequestAttributes.personId);
			// double walkScore =
			// parkingScoreManager.calcWalkScore(parkingOperationRequestAttributes.destCoordinate,
			// p.getKey(), parkingOperationRequestAttributes.personId,
			// parkingOperationRequestAttributes.parkingDurationInSeconds);
			// System.out.println(p.getKey().getId() + "\t" + p.getScore() +
			// "\t" + costScore + "\t" + walkScore);
			// }
			//
			// DebugLib.emptyFunctionForSettingBreakPoint();
			// }
			//
			// }
		}

		try {
			eventsManager.processEvent(new ParkingArrivalEvent(parkingOperationRequestAttributes.arrivalTime,
					selectedParking.getId(), parkingOperationRequestAttributes.personId,
					parkingOperationRequestAttributes.destCoordinate, -1.0* finalScore));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// yyyyyy the parking arrival event returns person id, not vehicle id.
		// Do we change to vehicle id, or add the vehicle
		// id to the event? In the code, the person id is used for the walk
		// distance. kai, jul'15

		return selectedParking;
	}

	private Collection<PC2Parking> getFilteredCollection(
			ParkingOperationRequestAttributes parkingOperationRequestAttributes, double distance) {
		Collection<PC2Parking> collection = getPublicParkingQuadTree().getDisk(
				parkingOperationRequestAttributes.destCoordinate.getX(),
				parkingOperationRequestAttributes.destCoordinate.getY(), distance);

		LinkedList<PC2Parking> deleteList = new LinkedList<>();

		for (PC2Parking parking : collection) {
			if (parking instanceof RentableParking) {
				RentableParking rp = (RentableParking) parking;

				if (!rp.isRentable(parkingOperationRequestAttributes.arrivalTime)) {
					deleteList.add(rp);
				} else {
					DebugLib.emptyFunctionForSettingBreakPoint();
				}
			}
		}
		collection.removeAll(deleteList);
		return collection;
	}

	// private synchronized void availablePublicParkingAtCityCentre() {
	// CoordImpl lindenHof = new CoordImpl(683235.0, 247497.0);
	// Collection<PC2Parking> collection2 =
	// getPublicParkingQuadTree().get(lindenHof.getX(), lindenHof.getY(), 1000);
	//
	// if (collection2.size() > 0) {
	// DebugLib.emptyFunctionForSettingBreakPoint();
	// }
	//
	// printParkingGroupSizes();
	// DebugLib.emptyFunctionForSettingBreakPoint();
	// }

	// private synchronized void printParkingGroupSizes() {
	// for (String groupName : publicParkingGroupQuadTrees.keySet()) {
	// System.out.println(groupName + "\t" +
	// publicParkingGroupQuadTrees.get(groupName).size());
	// }
	// }

	private synchronized void parkVehicle(PC2Parking parking) {
		int startAvailability = parking.getAvailableParkingCapacity();

		parking.parkVehicle();
		if (parking.getAvailableParkingCapacity() == 0) {
			boolean wasRemoved = false;
			wasRemoved = publicParkingsQuadTree.remove(parking.getCoordinate().getX(), parking.getCoordinate().getY(),
					parking);

			if (!wasRemoved) {
				DebugLib.stopSystemAndReportInconsistency(parking.getId().toString());
			}

			wasRemoved = publicParkingGroupQuadTrees.get(parking.getGroupName()).remove(parking.getCoordinate().getX(),
					parking.getCoordinate().getY(), parking);

			if (!wasRemoved) {
				DebugLib.stopSystemAndReportInconsistency(parking.getId().toString());
			}

			Collection<PC2Parking> collection = publicParkingsQuadTree.getDisk(parking.getCoordinate().getX(),
					parking.getCoordinate().getY(), 1);
			if (collection.size() == 1) {
				for (PC2Parking p : collection) {
					// yyyyyy no idea why we first test for "size of collection
					// == 1" and then iterate over it. kai, jul'15

					if (p.getId().toString().equalsIgnoreCase(parking.getId().toString())) {
						// (i.e. the found parking is not the same as the one
						// that should be at the coordinate; two parking
						// locations are
						// on top of each other)

						DebugLib.stopSystemAndReportInconsistency();
					}
				}
			}
		}

		int endAvailability = parking.getAvailableParkingCapacity();

		DebugLib.assertTrue(startAvailability - 1 == endAvailability, "not equal");

	}

	// public synchronized LinkedList<PC2Parking>
	// getNonFullParking(Collection<PC2Parking> parkings) {
	// LinkedList<PC2Parking> result = new LinkedList<PC2Parking>();
	// for (PC2Parking p : parkings) {
	// if (p.getAvailableParkingCapacity() > 0) {
	// result.add(p);
	// }
	// }
	// return result;
	// }
	// never used. kai, jul'15

	// TODO: make this method abstract
	public synchronized PC2Parking personCarDepartureEvent(
			ParkingOperationRequestAttributes parkingOperationRequestAttributes) {
		final Id<Person> personId = parkingOperationRequestAttributes.personId;
		Id<PC2Parking> parkingFacilityId = parkedVehicles.get(personId);
		PC2Parking parking = getAllParkings().get(parkingFacilityId);

		parkedVehicles.remove(personId);

		unParkVehicle(parking, parkingOperationRequestAttributes.arrivalTime
				+ parkingOperationRequestAttributes.parkingDurationInSeconds, personId);
		return parking;
	}

	public synchronized void scoreParkingOperation(ParkingOperationRequestAttributes parkingOperationRequestAttributes,
			PC2Parking parking) {
		double score = parkingScoreManager.calcScore(parkingOperationRequestAttributes.destCoordinate,
				parkingOperationRequestAttributes.arrivalTime,
				parkingOperationRequestAttributes.parkingDurationInSeconds, parking,
				parkingOperationRequestAttributes.personId, parkingOperationRequestAttributes.legIndex, false);
		parkingScoreManager.addScore(parkingOperationRequestAttributes.personId, score);
	}

	public synchronized void unParkVehicle(PC2Parking parking, double departureTime, Id<Person> personId) {
		// yyyy I would prefer if this could not be called publicly ... since it
		// allows to bypass parkedVehicles.remove(personId);
		// in personCarDepartureEvent(...). kai, jul'15

		if (parking == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		int startAvailability = parking.getAvailableParkingCapacity();

		parking.unparkVehicle();

		if (parking.getAvailableParkingCapacity() == 1) {
			if (!(parking instanceof PrivateParking)) {
				addParkingToQuadTree(publicParkingsQuadTree, parking);
				addParkingToQuadTree(publicParkingGroupQuadTrees.get(parking.getGroupName()), parking);
				// I could speculate that full parking lots are removed from the
				// quad tree, and it is re-added as soon
				// as space becomes available. But this is not commented, and it
				// also does not look like it would work in that way.
				// kai, jul'15
				// No, actually I now think that it works. When
				// remainingCapacity==0, then the parking is removed from the
				// list, and
				// when (again) ==1, it is re-inserted. In addition, it is
				// re-inserted in reset.
			}
		}

		int endAvailability = parking.getAvailableParkingCapacity();

		DebugLib.assertTrue(startAvailability + 1 == endAvailability, "not equal");

		eventsManager.processEvent(new ParkingDepartureEvent(departureTime, parking.getId(), personId));
	}

	public synchronized ParkingScoreManager getParkingScoreManager() {
		return parkingScoreManager;
	}

	public synchronized EventsManager getEventsManager() {
		return eventsManager;
	}

	public synchronized void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	public HashMap<Id<PC2Parking>, PC2Parking> getAllParkings() {
		return allParkings;
	}

	public void setAllParkings(HashMap<Id<PC2Parking>, PC2Parking> allParkings) {
		this.allParkings = allParkings;
	}

	// TODO: allso allow to filter by group the parkings

	// Allow to reprogramm the decision making process of the agent => provide
	// default module for decision making and new one,
	// which could also cope with EVs.

	// provide interface for proper integration.

	// also loading of data should

}
