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
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTime;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * Manages vehicles parking actions at facilities or freely on the street. I.e. keeps track of the capacity of the facilities. This class has
 * additional functionality:
 * - It can handle parking reservations.
 * - It triggers reporting of parking statistics.
 * - It can handle vehicles waiting for a parking space. *
 *
 * @author jbischoff, schlenther, Ricardo Ewert
 */
public class FacilityBasedParkingManager implements ParkingSearchManager {
	private static final Logger logger = LogManager.getLogger(FacilityBasedParkingManager.class);
	private static final int REPORTING_TIME_BIN_SIZE = 15 * 60;

	protected Map<Id<ActivityFacility>, ParkingFacilityInfo> infoByFacilityId = new HashMap<>();

	protected Map<Id<Link>, TreeMap<Double, Id<Vehicle>>> waitingVehiclesByLinkId = new HashMap<>();
	protected Map<Id<ActivityFacility>, ActivityFacility> parkingFacilitiesById;
	protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingFacilityLocationByVehicleId = new HashMap<>();
	protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingReservationByVehicleId = new HashMap<>();
	//stores the parking location of vehicles that are parked outside of facilities (e.g. at the side of a link. therefore, there are no capacity
	// checks)
	protected Map<Id<Vehicle>, Id<Link>> freeParkingLinkByVehicleId = new HashMap<>();
	protected Map<Id<Link>, Set<Id<ActivityFacility>>> parkingFacilitiesByLink = new HashMap<>();
	protected ParkingSearchConfigGroup psConfigGroup;
	private QSim qsim;

	//The following maps are used for reporting
	@Inject
	private ParkingStatsWriter writer;
	protected TreeMap<Integer, MutableLong> rejectedReservationsByTime = new TreeMap<>();
	protected TreeMap<Integer, MutableLong> foundParkingByTime = new TreeMap<>();
	protected TreeMap<Integer, MutableLong> unparkByTime = new TreeMap<>();
	private int reportingMaxSlotIndex;
	private int reportingMaxTime;

	@Inject
	public FacilityBasedParkingManager(Scenario scenario) {
		psConfigGroup = (ParkingSearchConfigGroup) scenario.getConfig().getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		parkingFacilitiesById = scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.ParkingStageInteractionType);

		logger.info(parkingFacilitiesById.toString());

		for (ActivityFacility fac : this.parkingFacilitiesById.values()) {
			initParkingFacility(fac);
		}

		initReporting(scenario);
	}

	private void initReporting(Scenario scenario) {
		this.reportingMaxTime = (int) scenario.getConfig().qsim().getEndTime().seconds();
		this.reportingMaxSlotIndex = (this.reportingMaxTime / REPORTING_TIME_BIN_SIZE) + 1;
	}

	private void initParkingFacility(ActivityFacility fac) {
		Id<Link> linkId = fac.getLinkId();
		Set<Id<ActivityFacility>> parkingOnLink = parkingFacilitiesByLink.getOrDefault(linkId, new HashSet<>());
		parkingOnLink.add(fac.getId());
		this.parkingFacilitiesByLink.put(linkId, parkingOnLink);
		this.waitingVehiclesByLinkId.put(linkId, new TreeMap<>());

		ActivityOption activityOption = fac.getActivityOptions().get(ParkingUtils.ParkingStageInteractionType);
		this.infoByFacilityId.put(fac.getId(), new ParkingFacilityInfo(activityOption));
	}

	@Override
	public boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		return linkIdHasAvailableParkingForVehicle(linkId, vehicleId);
	}

	/**
	 * Checks if it is possible if you can park at this link for the complete time.
	 */
	public boolean canParkAtThisFacilityUntilEnd(Id<Link> linkId, double now, double getOffDuration, double stopDuration, double pickUpDuration) {
		Set<Id<ActivityFacility>> facilities = this.parkingFacilitiesByLink.get(linkId);
		if (facilities == null) {
			//TODO really? if there is no facility we assume free parking with no time constraint.
			return false;
		}
		double totalDuration = getOffDuration + stopDuration + pickUpDuration;
		for (Id<ActivityFacility> facility : facilities) {
			double maxParkingDurationAtFacilityInSeconds =
				Optional.ofNullable(this.parkingFacilitiesById.get(facility).getAttributes().getAsMap().get("maxParkingDurationInHours"))
						.map(attribute -> 3600 * (double) attribute).orElse(Double.MAX_VALUE);

			if (maxParkingDurationAtFacilityInSeconds < totalDuration) {
				//Parking duration is limited, so we can't park here.
				return false;
			}

			ActivityOption parkingOptions = this.infoByFacilityId.get(facility).activityOption;
			if (parkingOptions.getOpeningTimes().isEmpty()) {
				//No opening times defined, so we can park here.
				return true;
			}

			OpeningTime firstOpeningTimes = parkingOptions.getOpeningTimes().first();
			//TODO do we really want this constraint? if parking facility has other opening times than 0-24, we can't park here.
			if ((firstOpeningTimes.getStartTime() == 0 && firstOpeningTimes.getEndTime() == 24 * 3600)) {
				if (firstOpeningTimes.getStartTime() <= now && firstOpeningTimes.getEndTime() >= now + totalDuration) {
					//Parking facility is open for the complete duration, so we can park here.
					return true;
				}
			}

		}
		//No parking facility is open for the complete duration, so we can't park here.
		return false;
	}

	/**
	 * Either parks the vehicle at a link freely (no capacity constraint) or at a facility (capacity constraint).
	 */
	private boolean linkIdHasAvailableParkingForVehicle(Id<Link> linkId, Id<Vehicle> vid) {
		if (!this.parkingFacilitiesByLink.containsKey(linkId)) {
			// No parking facility at this link. Either parking is allowed only at facilities or not.
			// If not, we can park freely, so link has available parking. (MATSim standard approach)
			// If yes, we can't park here.
			return !psConfigGroup.getCanParkOnlyAtFacilities();
		}
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.parkingFacilitiesByLink.get(linkId);
		for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
			if (this.infoByFacilityId.get(fac).park()) {
				this.parkingReservationByVehicleId.put(vid, fac);
				return true;
			}
		}
		return false;
	}

	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		if (this.parkingFacilityLocationByVehicleId.containsKey(vehicleId)) {
			//parked at facility
			return this.parkingFacilitiesById.get(this.parkingFacilityLocationByVehicleId.get(vehicleId)).getLinkId();
		} else {
			//parked freely
			return this.freeParkingLinkByVehicleId.getOrDefault(vehicleId, null);
		}
	}

	/**
	 * Parks a vehicle at a link. Either freely outside a facility or at a facility.
	 */
	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		return parkVehicleAtLink(vehicleId, linkId, time);
	}

	protected boolean parkVehicleAtLink(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.parkingFacilitiesByLink.get(linkId);
		if (parkingFacilitiesAtLink == null) {
			//park freely
			this.freeParkingLinkByVehicleId.put(vehicleId, linkId);
			return true;
		}
		//park at facility
		return parkVehicleInReservedFacility(vehicleId, linkId, time);
	}

	private boolean parkVehicleInReservedFacility(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		Id<ActivityFacility> fac = this.parkingReservationByVehicleId.remove(vehicleId);
		if (fac == null) {
			throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
				+ " arrival on link " + linkId + " with parking restriction");
		}

		Gbl.assertIf(parkingFacilitiesById.get(fac).getLinkId().equals(linkId));

		this.parkingFacilityLocationByVehicleId.put(vehicleId, fac);
		reportParking(time, fac);
		return true;
	}

	/**
	 * Unparks vehicle from a link. Either freely outside a facility or at a facility.
	 */
	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingFacilityLocationByVehicleId.containsKey(vehicleId)) {
			//unpark freely
			this.freeParkingLinkByVehicleId.remove(vehicleId);
		} else {
			//unpark at facility
			Id<ActivityFacility> fac = this.parkingFacilityLocationByVehicleId.remove(vehicleId);
			reportUnParking(time, fac);
		}
		return true;
	}

	private List<String> produceStatistics() {
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
		rejectedReservationsByTime.putIfAbsent(getTimeSlotIndex(now) * REPORTING_TIME_BIN_SIZE, new MutableLong(0));
		rejectedReservationsByTime.get(getTimeSlotIndex(now) * REPORTING_TIME_BIN_SIZE).increment();
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.reportingMaxTime) {
			return this.reportingMaxSlotIndex;
		}
		return ((int) time / REPORTING_TIME_BIN_SIZE);
	}

	/**
	 * Returns the duration of the staging activity of parking
	 */
	public double getParkStageActivityDuration() {
		return psConfigGroup.getParkduration();
	}

	@Override
	public void reset(int iteration) {
		infoByFacilityId.replaceAll((k, v) -> new ParkingFacilityInfo(v.activityOption));
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

	//@formatter:off
	/**
	 * For all links l with waiting vehicles:
	 * 		For all facilities f located at l:
	 * 			While there is a free capacity at f and there are waiting vehicles:
	 * 				Remove the first waiting vehicle from the list of waiting vehicles.
	 * 				Reserve a parking space for this vehicle.
	 * 				End the activity of the vehicle.
	 * 				Reschedule the activity end of the vehicle.
	 *
	 */
	//@formatter:on
	public void checkFreeCapacitiesForWaitingVehicles(QSim qSim, double now) {
		for (Id<Link> linkId : waitingVehiclesByLinkId.keySet()) {
			TreeMap<Double, Id<Vehicle>> vehicleIdByTime = waitingVehiclesByLinkId.get(linkId);
			if (vehicleIdByTime.isEmpty()) {
				break;
			}
			for (Id<ActivityFacility> fac : this.parkingFacilitiesByLink.get(linkId)) {
				int capacity = (int) this.parkingFacilitiesById.get(fac).getActivityOptions().get(ParkingUtils.ParkingStageInteractionType)
															   .getCapacity();

				while (this.infoByFacilityId.get(fac).occupation < capacity && !vehicleIdByTime.isEmpty()) {
					double startWaitingTime = vehicleIdByTime.firstKey();
					if (startWaitingTime > now) {
						break;
					}
					Id<Vehicle> vehcileId = vehicleIdByTime.remove(startWaitingTime);
					DynAgent agent = (DynAgent) qSim.getAgents().get(Id.createPersonId(vehcileId.toString()));
					reserveSpaceIfVehicleCanParkHere(vehcileId, linkId);
					agent.endActivityAndComputeNextState(now);
					qsim.rescheduleActivityEnd(agent);
				}
			}
		}
	}

	public void setQSim(QSim qSim) {
		qsim = qSim;
	}

	public void registerStayFromGetOffUntilGetIn(Id<Vehicle> vehcileId) {
		infoByFacilityId.get(parkingFacilityLocationByVehicleId.get(vehcileId)).staysFromGetOffUntilGetIn++;
	}

	public void registerParkingBeforeGetIn(Id<Vehicle> vehcileId) {
		this.infoByFacilityId.get(parkingFacilityLocationByVehicleId.get(vehcileId)).parkingBeforeGetInCount++;
	}

	private void reportParking(double time, Id<ActivityFacility> fac) {
		this.infoByFacilityId.get(fac).parkedVehiclesCount++;
		int timeSlot = getTimeSlotIndex(time) * REPORTING_TIME_BIN_SIZE;
		foundParkingByTime.putIfAbsent(timeSlot, new MutableLong(0));
		foundParkingByTime.get(timeSlot).increment();
	}

	private void reportUnParking(double time, Id<ActivityFacility> fac) {
		this.infoByFacilityId.get(fac).occupation--;
		int timeSlot = getTimeSlotIndex(time) * REPORTING_TIME_BIN_SIZE;
		unparkByTime.putIfAbsent(timeSlot, new MutableLong(0));
		unparkByTime.get(timeSlot).increment();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writer.writeStatsByFacility(produceStatistics(), event.getIteration());
		writer.writeStatsByTimesteps(produceTimestepsStatistics(), event.getIteration());
		reset(event.getIteration());
	}


	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		checkFreeCapacitiesForWaitingVehicles((QSim) event.getQueueSimulation(), event.getSimulationTime());
	}

	@Override
	public void notifyMobsimInitialized(final MobsimInitializedEvent e) {
		QSim qSim = (QSim) e.getQueueSimulation();
		setQSim(qSim);
	}

	protected static class ParkingFacilityInfo {
		protected long occupation = 0;
		protected final ActivityOption activityOption;
		protected long reservationRequests = 0;
		protected long rejectedParkingRequests = 0;
		protected long parkedVehiclesCount = 0;
		protected long waitingActivitiesCount = 0;
		protected long staysFromGetOffUntilGetIn = 0;
		protected long parkingBeforeGetInCount = 0;

		ParkingFacilityInfo(ActivityOption activityOption) {
			this.activityOption = activityOption;
		}

		protected boolean park() {
			reservationRequests++;
			//TODO check double vs long
			if (occupation >= activityOption.getCapacity()) {
				rejectedParkingRequests++;
				return false;
			}
			occupation++;
			return true;
		}
	}
}
