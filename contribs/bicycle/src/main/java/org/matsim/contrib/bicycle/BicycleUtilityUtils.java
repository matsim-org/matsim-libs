/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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
package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

/**
 * @author dziemke
 */
class BicycleUtilityUtils {

	static double computeLinkBasedScore( Link link, double marginalUtilityOfComfort_m, double marginalUtilityOfInfrastructure_m,
										 double marginalUtilityOfGradient_m_100m, double marginalUtilityOfUserDefinedNetworkAttribute_m,
										 String nameOfUserDefinedNetworkAttribute, double userDefinedNetworkAttributeDefaultValue) {
		String surface = (String) link.getAttributes().getAttribute(BicycleUtils.SURFACE);
		String type = (String) link.getAttributes().getAttribute("type");
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleUtils.CYCLEWAY);

		double distance = link.getLength();

		double comfortFactor = getComfortFactor(surface);
		double comfortScore = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance;

		double infrastructureFactor = getInfrastructureFactor(type, cyclewaytype);
		double infrastructureScore = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance;

		double gradient = getGradient(link);
		double gradientScore = marginalUtilityOfGradient_m_100m * gradient * distance;

		String userDefinedNetworkAttributeString;
		double userDefinedNetworkAttributeScore = 0.;
		if (nameOfUserDefinedNetworkAttribute != null) {
			userDefinedNetworkAttributeString = BicycleUtils.getUserDefinedNetworkAttribute(link, nameOfUserDefinedNetworkAttribute);
			double userDefinedNetworkAttributeFactor = getUserDefinedNetworkAttributeFactor(userDefinedNetworkAttributeString, userDefinedNetworkAttributeDefaultValue);
			userDefinedNetworkAttributeScore = marginalUtilityOfUserDefinedNetworkAttribute_m * (1. - userDefinedNetworkAttributeFactor) * distance;
		}

		return (infrastructureScore + comfortScore + gradientScore + userDefinedNetworkAttributeScore);
	}

	static double getGradient(Link link ) {

		if (!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()) return 0.;

		var fromZ = link.getFromNode().getCoord().getZ();
		var toZ = link.getToNode().getCoord().getZ();
		var gradient = (toZ - fromZ) / link.getLength();
		// No positive utility for downhill, only negative for uphill
		return Math.max(0, gradient);
	}

	// TODO Combine this with speeds?
	static double getComfortFactor( String surface ) {

		// This method included another if/els branch with some conditions on road types which could never be reached. The following comment was
		// written above this branch. Deleting it, because I don't know what it was supposed to do. janek may '23
		//
		// For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
		// For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg

		if (surface == null) return 1.0;

		return switch (surface) {
				case "paved", "asphalt" -> 1.0;
				case "concrete:lanes" -> .95;
				case "concrete_plates", "concrete:plates", "fine_gravel" -> .9;
				case "paving_stones", "paving_stones:35", "paving_stones:30" -> .8;
				case "compacted" -> .7;
				case "unpaved", "asphalt;paving_stones:35", "bricks", "gravel", "ground" -> .6;
				case "sett", "cobblestone;flattened", "cobblestone:flattened" -> .5;
				case "cobblestone", "stone", "grass", "compressed", "paving_stones:3" -> .4;
				case "cobblestone (bad)", "dirt", "earth", "wood", "pebblestone", "sand" -> .3;
				case "concrete" -> .1;
				default -> .85;
			};
	}

	static double getInfrastructureFactor( String type, String cyclewaytype ) {

		// The method was unreadable before, so I hope I got the logic right. basically this differentiates between explicit cycleway tags, where the
		// road type has an influence on the factor, i.e. cycling along a primary road without cycle lane is less attractive compared to a tertiary road.
		// On the other hand if cycleways are present the factor is always 0.95, exept the cycle tracks has steps (horrible) or the road type is a
		// cycleway anyway (very nice)
		// in case there is no road type a medium factor of 0.85 is assigned
		// janek may '23

		if (type == null) return 0.85;
		if (hasNoCycleway(cyclewaytype)) {
			return switch (type) {
				case "trunk" -> 0.05;
				case "primary", "primary_link" -> 0.1;
				case "secondary", "secondary_link" -> 0.3;
				case "tertiary", "tertiary_link" -> 0.4;
				case "unclassified" -> 0.9;
				default -> 0.95;
			};
		} else {
			return switch (type) {
				case "cycleway", "path" -> 1.0;
				case "steps" -> 0.1;
				default -> 0.95;
			};
		}
	}

	private static boolean hasNoCycleway(String cyclewayType) {
		return (cyclewayType == null || cyclewayType.equals("no") || cyclewayType.equals("none"));
	}

	static double getUserDefinedNetworkAttributeFactor( String userDefinedNetworkAttributeString, double userDefinedNetworkAttributeDefaultValue ) {
		double userDefinedNetworkAttributeFactor = userDefinedNetworkAttributeDefaultValue;

		if (userDefinedNetworkAttributeString != null) {
			userDefinedNetworkAttributeFactor = Double.parseDouble(userDefinedNetworkAttributeString);
		}
		return userDefinedNetworkAttributeFactor;
	}
}
