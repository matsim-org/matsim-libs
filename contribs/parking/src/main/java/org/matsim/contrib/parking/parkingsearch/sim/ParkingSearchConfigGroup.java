/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.sim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Arrays;
import java.util.Map;

public class ParkingSearchConfigGroup extends ReflectiveConfigGroup {

	private static final Logger log = LogManager.getLogger(ParkingSearchConfigGroup.class);

	public enum ParkingSearchManagerType {FacilityBasedParkingManager, LinkLengthBasedParkingManagerWithRandomInitialUtilisation, ZoneParkingManager}

	public static final String GROUP_NAME = "parkingSearch";

	public static final String UNPARKDURATION = "unparkDuration";
	private double unparkDuration = 60;

	private static final String PARKDURATION = "parkDuration";
	private double parkDuration = 60;

	private static final String AVGPARKINGSLOTLENGTH = "avgParkingSlotLength";
	private double avgParkingSlotLength = 10.00;

	private static final String PARKINGSEARCH_STRATEGY = "parkingSearchStrategy";
	private ParkingSearchStrategy parkingSearchStrategy = ParkingSearchStrategy.Random;

	private static final String PARKINGSEARCH_MANAGER = "parkingSearchManager";
	private ParkingSearchManagerType parkingSearchManagerType = ParkingSearchManagerType.FacilityBasedParkingManager;

	//yyyy this parameter is only read by the NearestParkingSpotSearchLogic. Should this really be a global parameter? paul, nov'24
	private static final String FRACTION_CAN_CHECK_FREE_CAPACITIES_IN_ADVANCED = "fractionCanCheckFreeCapacitiesInAdvanced";
	private double fractionCanCheckFreeCapacitiesInAdvanced = 0.;

	//yyyy this parameter is only read by the NearestParkingSpotSearchLogic. Should this really be a global parameter? paul, nov'24
	private static final String FRACTION_CAN_RESERVE_PARKING_IN_ADVANCED = "fractionCanReserveParkingInAdvanced";
	private double fractionCanReserveParkingInAdvanced = 0.;

	private static final String CAN_PARK_ONLY_AT_FACILITIES = "canParkOnlyAtFacilities";
	private boolean canParkOnlyAtFacilities = false;

	public ParkingSearchConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(UNPARKDURATION)
	public double getUnparkduration() {
		return unparkDuration;
	}

	@StringGetter(PARKDURATION)
	public double getParkduration() {
		return parkDuration;
	}

	//yyyy shouldn't this parameter be facility specific? paul, nov'24
	@StringGetter(AVGPARKINGSLOTLENGTH)
	public double getAvgparkingslotlength() {
		return avgParkingSlotLength;
	}

	@StringGetter(PARKINGSEARCH_STRATEGY)
	public ParkingSearchStrategy getParkingSearchStrategy() {
		return parkingSearchStrategy;
	}

	@StringGetter(PARKINGSEARCH_MANAGER)
	public ParkingSearchManagerType getParkingSearchManagerType() {
		return parkingSearchManagerType;
	}

	@StringGetter(FRACTION_CAN_CHECK_FREE_CAPACITIES_IN_ADVANCED)
	public double getFractionCanCheckFreeCapacitiesInAdvanced() {
		return fractionCanCheckFreeCapacitiesInAdvanced;
	}

	@StringGetter(FRACTION_CAN_RESERVE_PARKING_IN_ADVANCED)
	public double getFractionCanReserveParkingInAdvanced() {
		return fractionCanReserveParkingInAdvanced;
	}

	@StringGetter(CAN_PARK_ONLY_AT_FACILITIES)
	public boolean getCanParkOnlyAtFacilities() {
		return canParkOnlyAtFacilities;
	}

	@StringSetter(UNPARKDURATION)
	public void setUnparkDuration(double unparkDuration) {
		this.unparkDuration = unparkDuration;
	}

	@StringSetter(PARKDURATION)
	public void setParkDuration(double parkDuration) {
		this.parkDuration = parkDuration;
	}

	@StringSetter(AVGPARKINGSLOTLENGTH)
	public void setAvgParkingSlotLength(double avgParkingSlotLength) {
		this.avgParkingSlotLength = avgParkingSlotLength;
	}

	@StringSetter(PARKINGSEARCH_STRATEGY)
	public void setParkingSearchStrategy(ParkingSearchStrategy parkingSearchStrategy) {
		this.parkingSearchStrategy = parkingSearchStrategy;
	}

	@StringSetter(PARKINGSEARCH_MANAGER)
	public void setParkingSearchManagerType(ParkingSearchManagerType parkingSearchManagerType) {
		this.parkingSearchManagerType = parkingSearchManagerType;
	}

	@StringSetter(FRACTION_CAN_CHECK_FREE_CAPACITIES_IN_ADVANCED)
	public void setFractionCanCheckFreeCapacitiesInAdvanced(double fractionCanCheckFreeCapacitiesInAdvanced) {
		this.fractionCanCheckFreeCapacitiesInAdvanced = fractionCanCheckFreeCapacitiesInAdvanced;
	}

	@StringSetter(FRACTION_CAN_RESERVE_PARKING_IN_ADVANCED)
	public void setFractionCanReserveParkingInAdvanced(double fractionCanReserveParkingInAdvanced) {
		this.fractionCanReserveParkingInAdvanced = fractionCanReserveParkingInAdvanced;
	}

	@StringSetter(CAN_PARK_ONLY_AT_FACILITIES)
	public void setCanParkOnlyAtFacilities(boolean canParkOnlyAtFacilities) {
		this.canParkOnlyAtFacilities = canParkOnlyAtFacilities;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(UNPARKDURATION, "Duration to unpark a vehicle");
		map.put(PARKDURATION, "Duration to park a vehicle");
		map.put(PARKINGSEARCH_STRATEGY,
			"The strategy to find a parking slot. Possible strategies: " + Arrays.toString(ParkingSearchStrategy.values()));
		map.put(PARKINGSEARCH_MANAGER, "The type of the ParkingManager, may have the values: " + Arrays.toString(ParkingSearchManagerType.values()));
		map.put(FRACTION_CAN_CHECK_FREE_CAPACITIES_IN_ADVANCED, "Fraction of agents who can check free capacities in advanced. This is currently " +
			"developed for the FacilityBasedParkingManager");
		map.put(FRACTION_CAN_RESERVE_PARKING_IN_ADVANCED, "Fraction of agents who can reserve free capacities in advanced. This is currently " +
			"developed for the FacilityBasedParkingManager\"");
		map.put(CAN_PARK_ONLY_AT_FACILITIES, "Set if a vehicle can park only at given parking facilities or it can park freely at a link without a" +
			" " +
			"facility.");

		return map;
	}

	@Override
	protected void checkConsistency(Config config) {

		super.checkConsistency(config);

		if (getFractionCanCheckFreeCapacitiesInAdvanced() != 0. && !getParkingSearchManagerType().equals(ParkingSearchManagerType.FacilityBasedParkingManager)) {
			log.warn("Fraction of agents who can check free capacities in advanced has no impact on your selected ParkingSearchManagerType, " +
				"because" +
				" " +
				"it is only implemented for the FacilityBasedParkingManager.");
		}

		if (getFractionCanCheckFreeCapacitiesInAdvanced() + getFractionCanReserveParkingInAdvanced() > 1.0) {
			throw new RuntimeException("The sum of " + FRACTION_CAN_RESERVE_PARKING_IN_ADVANCED + " and " + FRACTION_CAN_CHECK_FREE_CAPACITIES_IN_ADVANCED + " is > 1.0. This should not happen.");
		}

	}

}
