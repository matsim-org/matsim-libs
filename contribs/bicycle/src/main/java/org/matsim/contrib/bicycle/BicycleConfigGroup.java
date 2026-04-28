/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

/**
 * @author smetzler, dziemke
 */
public final class BicycleConfigGroup extends ReflectiveConfigGroup {
	// necessary to have this public

	public enum MotorizedInteraction {
		NONE("none"),
		CAR_COUNT_ON_BICYCLE_LEAVE_LINK("carCountOnBicycleLeaveLink"),
		CARS_PASSED_BICYCLE_ON_LINK("carsPassedBicycleOnLink"),
		AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL("avgCarOccupancyDuringBicycleTraversal");

		private final String configValue;

		MotorizedInteraction(String configValue) {
			this.configValue = configValue;
		}

		public String getConfigValue() {
			return this.configValue;
		}

		static MotorizedInteraction fromConfigValue(String configValue) {
			for (MotorizedInteraction value : values()) {
				if (value.configValue.equals(configValue)) {
					return value;
				}
			}
			throw new IllegalArgumentException(
				"Unsupported motorizedInteraction: " + configValue
					+ ". Supported values are '" + NONE.configValue
					+ "', '" + CAR_COUNT_ON_BICYCLE_LEAVE_LINK.configValue
					+ "', '" + CARS_PASSED_BICYCLE_ON_LINK.configValue
					+ "' and '" + AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL.configValue + "'."
			);
		}
	}

	public static final String GROUP_NAME = "bicycle";

	private static final String INPUT_COMFORT = "marginalUtilityOfComfort_m";
	private static final String INPUT_INFRASTRUCTURE = "marginalUtilityOfInfrastructure_m";
	private static final String INPUT_GRADIENT = "marginalUtilityOfGradient_m_100m";
	private static final String BICYCLE_MODE = "bicycleMode";
	private static final String MOTORIZED_INTERACTION = "motorizedInteraction";

	private double marginalUtilityOfComfort;

	private double marginalUtilityOfInfrastructure;
	private double marginalUtilityOfGradient;
	private String bicycleMode = "bicycle";
	private MotorizedInteraction motorizedInteraction = MotorizedInteraction.NONE;

	public BicycleConfigGroup() {
		super(GROUP_NAME);
	}


	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(INPUT_COMFORT, "marginalUtilityOfSurfacetype"); //TODO: update these comments, whats does it?
		map.put(INPUT_INFRASTRUCTURE, "marginalUtilityOfStreettype"); //TODO: update these comments, whats does it?
		map.put(INPUT_GRADIENT, "marginalUtilityOfGradient"); //TODO: update these comments, whats does it?
		map.put(MOTORIZED_INTERACTION,
			"Defines the motorized interaction behavior. Possible values [none, carCountOnBicycleLeaveLink, carsPassedBicycleOnLink, avgCarOccupancyDuringBicycleTraversal]. "
				+ "Default: none.");
//		map.put(BICYCLE_INFRA_ATTRIBUTE,
//			"Network attribute used for bicycle infrastructure evaluation. "
//				+ "Supported values: 'cycleway' (legacy default) and 'bicycle_infra'.");
		return map;
	}


	private static final String BICYCLE_INFRA_ATTRIBUTE = "bicycleInfraAttribute";


	private BicycleUtils.BicycleInfraAttribute bicycleInfraAttribute
		= BicycleUtils.BicycleInfraAttribute.cycleway;

	@StringGetter(BICYCLE_INFRA_ATTRIBUTE)
	public BicycleUtils.BicycleInfraAttribute getBicycleInfraAttribute() {
		return bicycleInfraAttribute;
	}

	@StringSetter(BICYCLE_INFRA_ATTRIBUTE)
	public BicycleConfigGroup setBicycleInfraAttribute(BicycleUtils.BicycleInfraAttribute attr) {
		this.bicycleInfraAttribute = attr;
		return this;
	}


	/**
	 * This is something like "cobblestone" or "paved" or "sand".
	 */
	@StringSetter(INPUT_COMFORT)
	public BicycleConfigGroup setMarginalUtilityOfComfort_m(final double value) {
		this.marginalUtilityOfComfort = value;
		return this;
	}

	@StringGetter(INPUT_COMFORT)
	public double getMarginalUtilityOfComfort_m() {
		return this.marginalUtilityOfComfort;
	}

	/**
	 * This is something like "arterial" or "residential road" or "has separate bicycle lane".
	 */
	@StringSetter(INPUT_INFRASTRUCTURE)
	public BicycleConfigGroup setMarginalUtilityOfInfrastructure_m(final double value) {
		this.marginalUtilityOfInfrastructure = value;
		return this;
	}

	@StringGetter(INPUT_INFRASTRUCTURE)
	public double getMarginalUtilityOfInfrastructure_m() {
		return this.marginalUtilityOfInfrastructure;
	}

	@StringSetter(INPUT_GRADIENT)
	public BicycleConfigGroup setMarginalUtilityOfGradient_pct_m(final double value) {
		// yy I do not understand what the _m_100m exactly means.  IMO, there is a "per meter" missing (i.e. _m_100m_m, or maybe just _m and the rest in the documentation).
		this.marginalUtilityOfGradient = value;
		return this;
	}

	@StringGetter(INPUT_GRADIENT)
	public double getMarginalUtilityOfGradient_pct_m() {
		return this.marginalUtilityOfGradient;
	}

/// TODO Simon: bike is now used everywhere -> need to update example networks, plans and events
/// set TransportMode.bike instead of bicycleConfigGroup.getBicycleMode() in BicycleLinkSpeedCalculatorDefaultImpl, BicycleModule, ...
//	//	@Deprecated

	/// /	@StringGetter(BICYCLE_MODE)
	/// /	public String getBicycleMode() {
	/// /		return TransportMode.bike;
	/// /	}
//
//	@Deprecated
//	@StringSetter(BICYCLE_MODE)
//	public BicycleConfigGroup setBicycleMode(String bicycleMode) {
//		throw new RuntimeException("This setter is no longe supported; bike is now used everywhere.");
//	}
	@StringGetter(BICYCLE_MODE)
	public String getBicycleMode() {
		return this.bicycleMode;
	}

	@StringSetter(BICYCLE_MODE)
	public BicycleConfigGroup setBicycleMode(String bicycleMode) {
		this.bicycleMode = bicycleMode;
		return this;
	}

	@StringGetter(MOTORIZED_INTERACTION)
	public String getMotorizedInteraction() {
		return this.motorizedInteraction.getConfigValue();
	}

	public MotorizedInteraction getMotorizedInteractionType() {
		return this.motorizedInteraction;
	}

	public boolean isMotorizedInteraction() {
		return this.motorizedInteraction != MotorizedInteraction.NONE;
	}

	public boolean isCarCountOnBicycleLeaveLink() {
		return this.motorizedInteraction == MotorizedInteraction.CAR_COUNT_ON_BICYCLE_LEAVE_LINK;
	}

	public boolean isCarsPassedBicycleOnLink() {
		return this.motorizedInteraction == MotorizedInteraction.CARS_PASSED_BICYCLE_ON_LINK;
	}

	public boolean isAvgCarOccupancyDuringBicycleTraversal() {
		return this.motorizedInteraction == MotorizedInteraction.AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL;
	}

	@StringSetter(MOTORIZED_INTERACTION)
	public BicycleConfigGroup setMotorizedInteraction(String motorizedInteraction) {
		this.motorizedInteraction = MotorizedInteraction.fromConfigValue(motorizedInteraction);
		return this;
	}

	public BicycleConfigGroup setMotorizedInteractionType(MotorizedInteraction motorizedInteraction) {
		this.motorizedInteraction = motorizedInteraction;
		return this;
	}

	public BicycleConfigGroup setMotorizedInteraction(boolean motorizedInteraction) {
		this.motorizedInteraction = motorizedInteraction
			? MotorizedInteraction.CAR_COUNT_ON_BICYCLE_LEAVE_LINK
			: MotorizedInteraction.NONE;
		return this;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		switch (config.qsim().getVehiclesSource()) {
			case defaultVehicle -> {
				throw new RuntimeException("cannot use qsim.getVehiclesSource = defaultVehicle with bicycle contrib.  Instead use " +
					"modeVehicles ... or fromVehiclesData; it is important that bicycles have speeds.  See RunBicycleContribExample in matsim-code-examples.");
			}
			case modeVehicleTypesFromVehiclesData, fromVehiclesData -> {
			}
			default -> throw new IllegalStateException("Unexpected value: " + config.qsim().getVehiclesSource());
		}
		switch (config.routing().getAccessEgressType()) {
			case none -> {
				throw new RuntimeException("cannot use the bicycle contrib together with accessEgressType==none.  See " +
					"RunBicycleContribExample in matsim-code-examples.");
			}
			case accessEgressModeToLink, walkConstantTimeToLink, accessEgressModeToLinkPlusTimeConstant -> {
			}
			default -> throw new IllegalStateException("Unexpected value: " + config.routing().getAccessEgressType());
		}
	}
}
