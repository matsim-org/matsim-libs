///* *********************************************************************** *
// * project: org.matsim.*												   *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package org.matsim.contrib.bicycle;
//
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.matsim.core.config.ConfigGroup;
//
///**
// * @author smetzler, dziemke
// */
//public class BicycleConfigGroup extends ConfigGroup {
//
//	public static final String GROUP_NAME = "bicycle";
//
//	private static final String INPUT_NETWORK_ATTRIBUTE_FILE = "inputNetworkObjectattributeFile";
//	private static final String INPUT_COMFORT = "marginalUtilityOfComfort_m";
//	private static final String INPUT_INFRASTRUCTURE = "marginalUtilityOfInfrastructure_m";
//	private static final String INPUT_GRADIENT = "marginalUtilityOfGradient_m_100m";
//
//	//Please note that the following betas are betas for the whole route - so the whole leg.
//	//This is because in (Livingston, Beyer-Bartana, Ziemke, Bahamonde-Birke, 2019),
//	//the attributes that were tested were all route attributes.
//	//The choice put to the survey respondants was between two routes.
//	//The the survey respondants were told that they should assume the attributes
//	//shown were representative for the majority of the route.
//	//Please see PersonSpecificBicycleUtilityUtils.computeLinkBasedScore to see how
//	//these betas are applied to individual links.  - clivings April 2019
//
//	//Please see PersonSpecificBicycleUtilityUtils.getPavementComfortFactor to see how
//	// OSM tags and classifications get assigned to one of these 4 categories - clivings April 2019
//	private static final String INPUT_BETA_COBBLESTONE = "betaCobblestone";
//	private static final String INPUT_BETA_FLAGSTONES = "betaFlagstones";
//	private static final String INPUT_BETA_GRAVEL = "betaGravel";
//	private static final String INPUT_BETA_ASPHALT = "betaAsphalt";
//
//	private static final String INPUT_BETA_COBBLESTONE_AGEGROUP2 = "betaCobblestoneAgeGroup2";
//
//	public static enum BicycleScoringType {legBased, linkBased};
//
//	private String networkAttFile = null;
//	private double marginalUtilityOfComfort;
//	private double marginalUtilityOfInfrastructure;
//	private double marginalUtilityOfGradient;
//
//	private double betaCobblestone;
//	private double betaFlagstones;
//	private double betaGravel;
//	private double betaAsphalt;
//	private double betaCobblestoneAgeGroup2;
//	private BicycleScoringType bicycleScoringType = BicycleScoringType.legBased;
//
//	public BicycleConfigGroup() {
//		super(GROUP_NAME);
//	}
//
//	@Override
//	public final void addParam(final String key, final String value) {
//		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
//		if ( "null".equalsIgnoreCase( value ) ) return;
//
//		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
//			setNetworkAttFile(value);
//		} else if (INPUT_COMFORT.equals(key)) {
//			setMarginalUtilityOfComfort_m(Double.parseDouble(value));
//		} else if (INPUT_INFRASTRUCTURE.equals(key)) {
//			setMarginalUtilityOfInfrastructure_m(Double.parseDouble(value));
//		} else if (INPUT_GRADIENT.equals(key)) {
//			setMarginalUtilityOfGradient_m_100m(Double.parseDouble(value));
//		} else if (INPUT_BETA_COBBLESTONE.equals(key)) {
//			setBetaCobblestone(Double.parseDouble(value));
//		} else if (INPUT_BETA_FLAGSTONES.equals(key)) {
//			setBetaFlagstones(Double.parseDouble(value));
//		} else if (INPUT_BETA_GRAVEL.equals(key)) {
//			setBetaGravel(Double.parseDouble(value));
//		} else if (INPUT_BETA_ASPHALT.equals(key)) {
//			setBetaAsphalt(Double.parseDouble(value));
//		} else if (INPUT_BETA_COBBLESTONE_AGEGROUP2.equals(key)) {
//			setBetaCobblestoneAgeGroup2(Double.parseDouble(value));
//		} else {
//			throw new IllegalArgumentException(key);
//		}
//	}
//
//
//	@Override
//	public final String getValue(final String key) {
//		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
//			return getNetworkAttFile();
//		} else if (INPUT_COMFORT.equals(key)) {
//			return Double.toString(getMarginalUtilityOfComfort_m());
//		} else if (INPUT_INFRASTRUCTURE.equals(key)) {
//			return Double.toString(getMarginalUtilityOfInfrastructure_m());
//		} else if (INPUT_GRADIENT.equals(key)) {
//			return Double.toString(getMarginalUtilityOfGradient_m_100m());
//		} else if (INPUT_BETA_COBBLESTONE.equals(key)) {
//			return Double.toString(getBetaCobblestone());
//		} else if (INPUT_BETA_FLAGSTONES.equals(key)) {
//			return Double.toString(getBetaFlagstones());
//		} else if (INPUT_BETA_GRAVEL.equals(key)) {
//			return Double.toString(getBetaGravel());
//		} else if (INPUT_BETA_ASPHALT.equals(key)) {
//			return Double.toString(getBetaAsphalt());
//		} else if (INPUT_BETA_COBBLESTONE_AGEGROUP2.equals(key)) {
//			return Double.toString(getBetaCobblestoneAgeGroup2());
//		} else {
//			throw new IllegalArgumentException(key);
//		}
//	}
//
//	@Override
//	public final TreeMap<String, String> getParams() {
//		TreeMap<String, String> map = new TreeMap<>();
//		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, getValue(INPUT_NETWORK_ATTRIBUTE_FILE));
//		map.put(INPUT_COMFORT, getValue(INPUT_COMFORT));
//		map.put(INPUT_INFRASTRUCTURE, getValue(INPUT_INFRASTRUCTURE));
//		map.put(INPUT_GRADIENT, getValue(INPUT_GRADIENT));
//		map.put(INPUT_BETA_COBBLESTONE, getValue(INPUT_BETA_COBBLESTONE));
//		map.put(INPUT_BETA_FLAGSTONES, getValue(INPUT_BETA_FLAGSTONES));
//		map.put(INPUT_BETA_GRAVEL, getValue(INPUT_BETA_GRAVEL));
//		map.put(INPUT_BETA_ASPHALT, getValue(INPUT_BETA_ASPHALT));
//		map.put(INPUT_BETA_COBBLESTONE_AGEGROUP2, getValue(INPUT_BETA_COBBLESTONE_AGEGROUP2));
//		return map;
//	}
//
//	@Override
//	public final Map<String, String> getComments() {
//		Map<String,String> map = super.getComments();
//		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, "Path to a file containing information for the network's links (required file format: ObjectAttributes).");
//		map.put(INPUT_COMFORT, "marginalUtilityOfSurfacetype");
//		map.put(INPUT_INFRASTRUCTURE, "marginalUtilityOfStreettype");
//		map.put(INPUT_GRADIENT, "marginalUtilityOfGradient");
//		map.put(INPUT_BETA_COBBLESTONE, "betaCobblestone");
//		map.put(INPUT_BETA_FLAGSTONES, "betaFlagstones");
//		map.put(INPUT_BETA_GRAVEL, "betaGravel");
//		map.put(INPUT_BETA_ASPHALT, "betaAsphalt");
//		map.put(INPUT_BETA_COBBLESTONE_AGEGROUP2, "betaCobblestoneAgeGroup2");
//		return map;
//	}
//	void setNetworkAttFile(String file) {
//		this.networkAttFile = file;
//	}
//
//	public String getNetworkAttFile() {
//		return this.networkAttFile;
//	}
//
//	public void setMarginalUtilityOfComfort_m(final double value) {
//		this.marginalUtilityOfComfort = value;
//	}
//
//	public double getMarginalUtilityOfComfort_m() {
//		return this.marginalUtilityOfComfort;
//	}
//
//	public void setMarginalUtilityOfInfrastructure_m(final double value) {
//		this.marginalUtilityOfInfrastructure = value;
//	}
//
//	public double getMarginalUtilityOfInfrastructure_m() {
//		return this.marginalUtilityOfInfrastructure;
//	}
//
//	public void setMarginalUtilityOfGradient_m_100m(final double value) {
//		this.marginalUtilityOfGradient = value;
//	}
//
//	public double getMarginalUtilityOfGradient_m_100m() {
//		return this.marginalUtilityOfGradient;
//	}
//
//	//Betas for pavements
//
//	//Cobblstone
//	public void setBetaCobblestone(final double value) {
//		this.betaCobblestone = value;
//	}
//
//	public double getBetaCobblestone() {
//		return this.betaCobblestone;
//	}
//	//Flagstones
//	public void setBetaFlagstones(final double value) {
//		this.betaFlagstones = value;
//	}
//
//	public double getBetaFlagstones() {
//		return this.betaFlagstones;
//	}
//	//Gravel
//	public void setBetaGravel(final double value) {
//		this.betaGravel = value;
//	}
//
//	public double getBetaGravel() {
//		return this.betaGravel;
//	}
//	//Asphalt
//	public void setBetaAsphalt(final double value) {
//		this.betaAsphalt = value;
//	}
//
//	public double getBetaAsphalt() {
//		return this.betaAsphalt;
//	}
//
//	//Interaction betas, or "taste variations"
//
//	//AgeGroup2 doesn't mind cobblestone as much as other age groups
//	public void setBetaCobblestoneAgeGroup2(final double value) {
//		this.betaCobblestoneAgeGroup2 = value;
//	}
//
//	public double getBetaCobblestoneAgeGroup2() {
//		return this.betaCobblestoneAgeGroup2;
//	}
//
//	//Bicycle Scoring Types
//	public void setBicycleScoringType(final BicycleScoringType value) {
//		this.bicycleScoringType = value;
//	}
//
//	public BicycleScoringType getBicycleScoringType() {
//		return this.bicycleScoringType;
//	}
//}
