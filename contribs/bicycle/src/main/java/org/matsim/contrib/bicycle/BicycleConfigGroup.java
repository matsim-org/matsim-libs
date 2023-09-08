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

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

/**
 * @author smetzler, dziemke
 */
public final class BicycleConfigGroup extends ReflectiveConfigGroup {
	// necessary to have this public

	public static final String GROUP_NAME = "bicycle";

	private static final String INPUT_COMFORT = "marginalUtilityOfComfort_m";
	private static final String INPUT_INFRASTRUCTURE = "marginalUtilityOfInfrastructure_m";
	private static final String INPUT_GRADIENT = "marginalUtilityOfGradient_m_100m";
	private static final String USER_DEFINED_NETWORK_ATTRIBUTE_MARGINAL_UTILITY = "marginalUtilityOfUserDefinedNetworkAttribute_m";
	private static final String USER_DEFINED_NETWORK_ATTRIBUTE_NAME = "userDefinedNetworkAttributeName";
	private static final String USER_DEFINED_NETWORK_ATTRIBUTE_DEFAULT_VALUE = "userDefinedNetworkAttributeDefaultValue";
	private static final String MAX_BICYCLE_SPEED_FOR_ROUTING = "maxBicycleSpeedForRouting";
	private static final String BICYCLE_MODE = "bicycleMode";
	private static final String MOTORIZED_INTERACTION = "motorizedInteraction";

	private double marginalUtilityOfComfort;

	@Deprecated
	@StringGetter(MAX_BICYCLE_SPEED_FOR_ROUTING)
	public double getMaxBicycleSpeedForRouting() {
		return this.maxBicycleSpeedForRouting;
	}
	private double marginalUtilityOfInfrastructure;
	private double marginalUtilityOfGradient;
	private double marginalUtilityOfUserDefinedNetworkAttribute;
	private String userDefinedNetworkAttributeName;
	private double userDefinedNetworkAttributeDefaultValue;
//	private BicycleScoringType bicycleScoringType = BicycleScoringType.legBased;
	private double maxBicycleSpeedForRouting = 25.0/3.6;
	private String bicycleMode = "bicycle";
	private boolean motorizedInteraction = false;

	public BicycleConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_COMFORT, "marginalUtilityOfSurfacetype");
		map.put(INPUT_INFRASTRUCTURE, "marginalUtilityOfStreettype");
		map.put(INPUT_GRADIENT, "marginalUtilityOfGradient");
		map.put(USER_DEFINED_NETWORK_ATTRIBUTE_MARGINAL_UTILITY, "marginalUtilityOfUserDefinedNetworkAttribute");
		map.put(USER_DEFINED_NETWORK_ATTRIBUTE_NAME, "userDefinedNetworkAttributeName");
		map.put(USER_DEFINED_NETWORK_ATTRIBUTE_DEFAULT_VALUE, "userDefinedNetworkAttributeDefaultValue");
		map.put(MAX_BICYCLE_SPEED_FOR_ROUTING, "maxBicycleSpeed");
		return map;
	}
	@StringSetter( INPUT_COMFORT )
	public BicycleConfigGroup setMarginalUtilityOfComfort_m( final double value ) {
		this.marginalUtilityOfComfort = value;
		return this;
	}
	@StringGetter( INPUT_COMFORT )
	public double getMarginalUtilityOfComfort_m() {
		return this.marginalUtilityOfComfort;
	}
	@StringSetter( INPUT_INFRASTRUCTURE )
	public BicycleConfigGroup setMarginalUtilityOfInfrastructure_m( final double value ) {
		this.marginalUtilityOfInfrastructure = value;
		return this;
	}
	@StringGetter( INPUT_INFRASTRUCTURE )
	public double getMarginalUtilityOfInfrastructure_m() {
		return this.marginalUtilityOfInfrastructure;
	}
	@StringSetter( INPUT_GRADIENT )
	public BicycleConfigGroup setMarginalUtilityOfGradient_m_100m( final double value ) {
		this.marginalUtilityOfGradient = value;
		return this;
	}
	@StringGetter( INPUT_GRADIENT )
	public double getMarginalUtilityOfGradient_m_100m() {
		return this.marginalUtilityOfGradient;
	}
	@StringSetter(USER_DEFINED_NETWORK_ATTRIBUTE_MARGINAL_UTILITY)
	public BicycleConfigGroup setMarginalUtilityOfUserDefinedNetworkAttribute_m(final double value) {
		this.marginalUtilityOfUserDefinedNetworkAttribute = value;
		return this;
	}
	@StringGetter(USER_DEFINED_NETWORK_ATTRIBUTE_MARGINAL_UTILITY)
	public double getMarginalUtilityOfUserDefinedNetworkAttribute_m() {
		return this.marginalUtilityOfUserDefinedNetworkAttribute;
	}
	@StringSetter(USER_DEFINED_NETWORK_ATTRIBUTE_NAME)
	public BicycleConfigGroup setUserDefinedNetworkAttributeName(String value) {
		this.userDefinedNetworkAttributeName = value;
		return this;
	}
	@StringGetter(USER_DEFINED_NETWORK_ATTRIBUTE_NAME)
	public String getUserDefinedNetworkAttributeName() {
		return this.userDefinedNetworkAttributeName;
	}
	@StringSetter(USER_DEFINED_NETWORK_ATTRIBUTE_DEFAULT_VALUE)
	public BicycleConfigGroup setUserDefinedNetworkAttributeDefaultValue(double value) {
		this.userDefinedNetworkAttributeDefaultValue = value;
		return this;
	}
	@StringGetter(USER_DEFINED_NETWORK_ATTRIBUTE_DEFAULT_VALUE)
	public double getUserDefinedNetworkAttributeDefaultValue() {
		return this.userDefinedNetworkAttributeDefaultValue;
	}
//	public BicycleConfigGroup setBicycleScoringType( final BicycleScoringType value ) {
//		this.bicycleScoringType = value;
//		return this;
//	}
//	public BicycleScoringType getBicycleScoringType() {
//		return this.bicycleScoringType;
//	}

	@StringSetter( MAX_BICYCLE_SPEED_FOR_ROUTING )
	@Deprecated
	public BicycleConfigGroup setMaxBicycleSpeedForRouting( final double value ) {
		this.maxBicycleSpeedForRouting = value;
		return this;
	}

//	public enum BicycleScoringType {legBased, @Deprecated linkBased}
	@StringGetter( BICYCLE_MODE )
	public String getBicycleMode() {
		return this.bicycleMode;
	}
	@StringSetter( BICYCLE_MODE )
	public BicycleConfigGroup setBicycleMode( String bicycleMode ) {
		this.bicycleMode = bicycleMode;
		return this;
	}
	@StringGetter( MOTORIZED_INTERACTION )
	public boolean isMotorizedInteraction() {
		return motorizedInteraction;
	}
	@StringSetter( MOTORIZED_INTERACTION )
	public BicycleConfigGroup setMotorizedInteraction( boolean motorizedInteraction ) {
		this.motorizedInteraction = motorizedInteraction;
		return this;
	}
}
