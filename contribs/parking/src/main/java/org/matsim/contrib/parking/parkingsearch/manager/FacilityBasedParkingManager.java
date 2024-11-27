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

package org.matsim.contrib.parking.parkingsearch.manager;

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author jbischoff, schlenther, Ricardo Ewert
 */
public class FacilityBasedParkingManager implements ParkingSearchManager {
	private static final Logger logger = LogManager.getLogger(FacilityBasedParkingManager.class);

	protected Map<Id<Link>, Integer> capacity = new HashMap<>();
	protected Map<Id<ActivityFacility>, ParkingFacilityInfo> infoByFacilityId = new HashMap<>();

	protected Map<Id<Link>, TreeMap<Double, Id<Vehicle>>> waitingVehiclesByLinkId = new HashMap<>();
	protected TreeMap<Integer, MutableLong> rejectedReservationsByTime = new TreeMap<>();
	protected TreeMap<Integer, MutableLong> foundParkingByTime = new TreeMap<>();
	protected TreeMap<Integer, MutableLong> unparkByTime = new TreeMap<>();
	protected Map<Id<ActivityFacility>, ActivityFacility> parkingFacilitiesById;
	protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingLocations = new HashMap<>();
	protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingReservation = new HashMap<>();
	protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();
	protected Map<Id<Link>, Set<Id<ActivityFacility>>> parkingFacilitiesByLink = new HashMap<>();
	protected ParkingSearchConfigGroup psConfigGroup;
	private QSim qsim;
	private final int maxSlotIndex;
	private final int maxTime;
	private final int timeBinSize;
	private final int startTime;

	protected static class ParkingFacilityInfo {
		protected long occupation = 0;
		protected long reservationRequests = 0;
		protected long rejectedParkingRequests = 0;
		protected long parkedVehiclesCount = 0;
		protected long waitingActivitiesCount = 0;
		protected long staysFromGetOffUntilGetIn = 0;
		protected long parkingBeforeGetInCount = 0;
	}

	@Inject
	public FacilityBasedParkingManager(Scenario scenario) {
		psConfigGroup = (ParkingSearchConfigGroup) scenario.getConfig().getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		parkingFacilitiesById = scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.ParkingStageInteractionType);

		logger.info(parkingFacilitiesById.toString());

		this.timeBinSize = 15 * 60;
		this.maxTime = 24 * 3600 - 1;
		this.maxSlotIndex = (this.maxTime / this.timeBinSize) + 1;
		this.startTime = 9 * 3600; //TODO yyyy? this is a magic variable and should either be deleted or configurable, paul nov '24

		for (ActivityFacility fac : this.parkingFacilitiesById.values()) {
			initParkingFacility(fac);
		}
		initReporting();
	}

	private void initReporting() {
		int slotIndex = getTimeSlotIndex(startTime);
		while (slotIndex <= maxSlotIndex) {
			rejectedReservationsByTime.put(slotIndex * timeBinSize, new MutableLong(0));
			foundParkingByTime.put(slotIndex * timeBinSize, new MutableLong(0));
			unparkByTime.put(slotIndex * timeBinSize, new MutableLong(0));
			slotIndex++;
		}
	}

	private void initParkingFacility(ActivityFacility fac) {
		Id<Link> linkId = fac.getLinkId();
		Set<Id<ActivityFacility>> parkingOnLink = new HashSet<>();
		if (this.parkingFacilitiesByLink.containsKey(linkId)) {
			parkingOnLink = this.parkingFacilitiesByLink.get(linkId);
		}
		parkingOnLink.add(fac.getId());
		this.parkingFacilitiesByLink.put(linkId, parkingOnLink);
		this.waitingVehiclesByLinkId.computeIfAbsent(linkId, (k) -> new TreeMap<>());

		this.infoByFacilityId.put(fac.getId(), new ParkingFacilityInfo());
	}

	@Override
	public boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		return linkIdHasAvailableParkingForVehicle(linkId, vehicleId);
	}

	/**
	 * Checks if it is possible if you can park at this link for the complete time.
	 *
	 * @param linkId
	 * @param stopDuration
	 * @param getOffDuration
	 * @param pickUpDuration
	 * @param now
	 * @return
	 */
	public boolean canParkAtThisFacilityUntilEnd(Id<Link> linkId, double stopDuration, double getOffDuration, double pickUpDuration, double now) {
		Set<Id<ActivityFacility>> facilities = this.parkingFacilitiesByLink.get(linkId);
		if (facilities != null) {
			double totalNeededParkingDuration = getOffDuration + stopDuration + pickUpDuration;
			for (Id<ActivityFacility> facility : facilities) {
				double maxParkingDurationAtFacilityInHours = Double.MAX_VALUE;
				if (this.parkingFacilitiesById.get(facility).getAttributes().getAsMap().containsKey("maxParkingDurationInHours")) {
					maxParkingDurationAtFacilityInHours = 3600 * (double) this.parkingFacilitiesById.get(facility).getAttributes().getAsMap().get(
						"maxParkingDurationInHours");
				}
				if (maxParkingDurationAtFacilityInHours > totalNeededParkingDuration) {
					ActivityOption parkingOptions = this.parkingFacilitiesById.get(facility).getActivityOptions().get("parking");
					if (!parkingOptions.getOpeningTimes().isEmpty()) {
						if ((parkingOptions.getOpeningTimes().first().getStartTime() == 0 && parkingOptions.getOpeningTimes().first()
																										   .getEndTime() == 24 * 3600)) {
							if (parkingOptions.getOpeningTimes().first().getStartTime() <= now && parkingOptions.getOpeningTimes().first()
																												.getEndTime() >= now + totalNeededParkingDuration) {
								return true;
							}
						}
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean linkIdHasAvailableParkingForVehicle(Id<Link> linkId, Id<Vehicle> vid) {
		// LogManager.getLogger(getClass()).info("link "+linkId+" vehicle "+vid);
		if (!this.parkingFacilitiesByLink.containsKey(linkId) && !psConfigGroup.getCanParkOnlyAtFacilities()) {
			// this implies: If no parking facility is present, we suppose that
			// we can park freely (i.e. the matsim standard approach)
			// it also means: a link without any parking spaces should have a
			// parking facility with 0 capacity.
			// LogManager.getLogger(getClass()).info("link not listed as parking
			// space, we will say yes "+linkId);

			return true;
		} else if (!this.parkingFacilitiesByLink.containsKey(linkId)) {
			return false;
		}
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.parkingFacilitiesByLink.get(linkId);
		for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
			double cap = this.parkingFacilitiesById.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType)
												   .getCapacity();
			this.infoByFacilityId.get(fac).reservationRequests++;
			if (this.infoByFacilityId.get(fac).occupation < cap) {
				// LogManager.getLogger(getClass()).info("occ:
				// "+this.occupation.get(fac).toString()+" cap: "+cap);
				this.infoByFacilityId.get(fac).occupation++;
				this.parkingReservation.put(vid, fac);

				return true;
			}
			this.infoByFacilityId.get(fac).rejectedParkingRequests++;
		}
		return false;
	}

	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		if (this.parkingLocations.containsKey(vehicleId)) {
			return this.parkingFacilitiesById.get(this.parkingLocations.get(vehicleId)).getLinkId();
		} else {
			return this.parkingLocationsOutsideFacilities.getOrDefault(vehicleId, null);
		}
	}

	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		return parkVehicleAtLink(vehicleId, linkId, time);
	}

	protected boolean parkVehicleAtLink(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.parkingFacilitiesByLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
			return true;
		} else {
			Id<ActivityFacility> fac = this.parkingReservation.remove(vehicleId);
			if (fac != null) {
				this.parkingLocations.put(vehicleId, fac);
				this.infoByFacilityId.get(fac).parkedVehiclesCount++;
				int timeSlot = getTimeSlotIndex(time) * timeBinSize;
				foundParkingByTime.putIfAbsent(timeSlot, new MutableLong(0));
				foundParkingByTime.get(timeSlot).increment();
				return true;
			} else {
				throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
					+ " arrival on link " + linkId + " with parking restriction");
			}
		}
	}

	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingLocations.containsKey(vehicleId)) {
			this.parkingLocationsOutsideFacilities.remove(vehicleId);
			return true;

			// we assume the person parks somewhere else
		} else {
			Id<ActivityFacility> fac = this.parkingLocations.remove(vehicleId);
			this.infoByFacilityId.get(fac).occupation--;
			int timeSlot = getTimeSlotIndex(time) * timeBinSize;
			unparkByTime.putIfAbsent(timeSlot, new MutableLong(0));
			unparkByTime.get(timeSlot).increment();
			return true;
		}
	}

	@Override
	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		for (Entry<Id<ActivityFacility>, ParkingFacilityInfo> e : this.infoByFacilityId.entrySet()) {
			Id<Link> linkId = this.parkingFacilitiesById.get(e.getKey()).getLinkId();
			double capacity = this.parkingFacilitiesById.get(e.getKey()).getActivityOptions()
														.get(ParkingUtils.ParkingStageInteractionType).getCapacity();
			double x = this.parkingFacilitiesById.get(e.getKey()).getCoord().getX();
			double y = this.parkingFacilitiesById.get(e.getKey()).getCoord().getY();

			String facilityId = e.getKey().toString();
			ParkingFacilityInfo info = e.getValue();
			String s =
				linkId.toString() + ";" + x + ";" + y + ";" + facilityId + ";" + capacity + ";" + info.occupation + ";" + info.reservationRequests +
					";" + info.parkedVehiclesCount + ";" + info.rejectedParkingRequests + ";" + info.waitingActivitiesCount + ";" +
					info.staysFromGetOffUntilGetIn + ";" + info.parkingBeforeGetInCount;
			stats.add(s);
		}
		return stats;
	}

	public List<String> produceTimestepsStatistics() {
		List<String> stats = new ArrayList<>();
		for (int time : rejectedReservationsByTime.keySet()) {

			String s = Time.writeTime(time, Time.TIMEFORMAT_HHMM) + ";" + rejectedReservationsByTime.get(time) + ";" + foundParkingByTime.get(
				time) + ";" + unparkByTime.get(time);
			stats.add(s);

		}
		return stats;
	}

	public double getNrOfAllParkingSpacesOnLink(Id<Link> linkId) {
		double allSpaces = 0;
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.parkingFacilitiesByLink.get(linkId);
		if (!(parkingFacilitiesAtLink == null)) {
			for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
				allSpaces += this.parkingFacilitiesById.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity();
			}
		}
		return allSpaces;
	}

	public double getNrOfFreeParkingSpacesOnLink(Id<Link> linkId) {
		double allFreeSpaces = 0;
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.parkingFacilitiesByLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			return 0;
		} else {
			for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
				int cap = (int) this.parkingFacilitiesById.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity();
				allFreeSpaces += (cap - this.infoByFacilityId.get(fac).occupation);
			}
		}
		return allFreeSpaces;
	}

	public Map<Id<ActivityFacility>, ActivityFacility> getParkingFacilitiesById() {
		return this.parkingFacilitiesById;
	}

	public void registerRejectedReservation(double now) {
		rejectedReservationsByTime.get(getTimeSlotIndex(now) * timeBinSize).increment();
	}

	public TreeSet<Integer> getTimeSteps() {
		TreeSet<Integer> timeSteps = new TreeSet<>();
		int slotIndex = 0;
		while (slotIndex <= maxSlotIndex) {
			timeSteps.add(slotIndex * timeBinSize);
			slotIndex++;
		}
		return timeSteps;
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int) time / this.timeBinSize);
	}

	/**
	 * Gives the duration of the staging activity of parking
	 *
	 * @return
	 */
	public double getParkStageActivityDuration() {
		return psConfigGroup.getParkduration();
	}

	/**
	 * Gives the duration of the staging activity of unparking
	 *
	 * @return
	 */
	public double getUnParkStageActivityDuration() {
		return psConfigGroup.getUnparkduration();
	}

	@Override
	public void reset(int iteration) {
		infoByFacilityId.replaceAll((k, v) -> new ParkingFacilityInfo());
		waitingVehiclesByLinkId.clear();
	}

	public void addVehicleForWaitingForParking(Id<Link> linkId, Id<Vehicle> vehicleId, double now) {
//		System.out.println(now + ": vehicle " +vehicleId.toString() + " starts waiting here: " + linkId.toString());
		waitingVehiclesByLinkId.get(linkId).put(now + getParkStageActivityDuration() + 1, vehicleId);
		for (Id<ActivityFacility> fac : this.parkingFacilitiesByLink.get(linkId)) {
			this.infoByFacilityId.get(fac).waitingActivitiesCount++;
			break;
		}

	}

	public void checkFreeCapacitiesForWaitingVehicles(QSim qSim, double now) {
		for (Id<Link> linkId : waitingVehiclesByLinkId.keySet()) {
			if (!waitingVehiclesByLinkId.get(linkId).isEmpty()) {
				for (Id<ActivityFacility> fac : this.parkingFacilitiesByLink.get(linkId)) {
					int cap = (int) this.parkingFacilitiesById.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType)
															  .getCapacity();
					while (this.infoByFacilityId.get(fac).occupation < cap && !waitingVehiclesByLinkId.get(linkId).isEmpty()) {
						double startWaitingTime = waitingVehiclesByLinkId.get(linkId).firstKey();
						if (startWaitingTime > now) {
							break;
						}
						Id<Vehicle> vehcileId = waitingVehiclesByLinkId.get(linkId).remove(startWaitingTime);
						DynAgent agent = (DynAgent) qSim.getAgents().get(Id.createPersonId(vehcileId.toString()));
						reserveSpaceIfVehicleCanParkHere(vehcileId, linkId);
						agent.endActivityAndComputeNextState(now);
						qsim.rescheduleActivityEnd(agent);
					}
				}
			}
		}
	}

	public void setQSim(QSim qSim) {
		qsim = qSim;
	}

	public void registerStayFromGetOffUntilGetIn(Id<Vehicle> vehcileId) {
		this.infoByFacilityId.get(parkingLocations.get(vehcileId)).staysFromGetOffUntilGetIn++;
	}

	public void registerParkingBeforeGetIn(Id<Vehicle> vehcileId) {
		this.infoByFacilityId.get(parkingLocations.get(vehcileId)).parkingBeforeGetInCount++;
	}
}
