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
	
	static double computeLinkBasedScore( Link link, double marginalUtilityOfComfort_m,
							 double marginalUtilityOfInfrastructure_m, double marginalUtilityOfGradient_m_100m ) {
		String surface = (String) link.getAttributes().getAttribute(BicycleLabels.SURFACE);
		String type = (String) link.getAttributes().getAttribute("type");
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleLabels.CYCLEWAY);

		double distance = link.getLength();
		
		double comfortFactor = BicycleUtilityUtils.getComfortFactor(surface, type);
		double comfortDisutility = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance;
		
		double infrastructureFactor = BicycleUtilityUtils.getInfrastructureFactor(type, cyclewaytype);
		double infrastructureDisutility = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance;
		
		double gradientFactor = BicycleUtilityUtils.getGradientFactor(link);
		double gradientDisutility = marginalUtilityOfGradient_m_100m * gradientFactor * distance;
		return (infrastructureDisutility + comfortDisutility + gradientDisutility);
	}
	
	static double getGradientFactor( Link link ) {
		double gradient = 0.;
		Double fromNodeZ = link.getFromNode().getCoord().getZ();
		Double toNodeZ = link.getToNode().getCoord().getZ();
		if ((fromNodeZ != null) && (toNodeZ != null)) {
			if (toNodeZ > fromNodeZ) { // No positive utility for downhill, only negative for uphill
				gradient = (toNodeZ - fromNodeZ) / link.getLength();
			}
		}
		return gradient;
	}

	// TODO Combine this with speeds?
	static double getComfortFactor( String surface, String type ) {
		double comfortFactor = 1.0;
		if (surface != null) {
			switch (surface) {
			case "paved":
			case "asphalt": comfortFactor = 1.0; break;
			case "cobblestone": comfortFactor = .40; break;
			case "cobblestone (bad)": comfortFactor = .30; break;
			case "sett": comfortFactor = .50; break;
			case "cobblestone;flattened":
			case "cobblestone:flattened": comfortFactor = .50; break;
			case "concrete": comfortFactor = .100; break;
			case "concrete:lanes": comfortFactor = .95; break;
			case "concrete_plates":
			case "concrete:plates": comfortFactor = .90; break;
			case "paving_stones": comfortFactor = .80; break;
			case "paving_stones:35":
			case "paving_stones:30": comfortFactor = .80; break;
			case "unpaved": comfortFactor = .60; break;
			case "compacted": comfortFactor = .70; break;
			case "dirt":
			case "earth": comfortFactor = .30; break;
			case "fine_gravel": comfortFactor = .90; break;
			case "gravel":
			case "ground": comfortFactor = .60; break;
			case "wood":
			case "pebblestone":
			case "sand": comfortFactor = .30; break;
			case "bricks": comfortFactor = .60; break;
			case "stone":
			case "grass":
			case "compressed": comfortFactor = .40; break;
			case "asphalt;paving_stones:35": comfortFactor = .60; break;
			case "paving_stones:3": comfortFactor = .40; break;
			default: comfortFactor = .85;
			}
		} else {
			// For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
			// For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg
			if (type != null) {
				if (type.equals("primary") || type.equals("primary_link") || type.equals("secondary") || type.equals("secondary_link")) {
					comfortFactor = 1.0;
				}
			}
		}
		return comfortFactor;
	}

	static double getInfrastructureFactor( String type, String cyclewaytype ) {
		double infrastructureFactor = 1.0;
		if (type != null) {
			if (type.equals("trunk")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .05;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("primary") || type.equals("primary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .10;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("secondary") || type.equals("secondary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .30;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("tertiary") || type.equals("tertiary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .40;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("unclassified")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .90;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("unclassified")) {
				infrastructureFactor = .95;
			} else if (type.equals("service") || type.equals("living_street") || type.equals("minor")) {
				infrastructureFactor = .95;
			} else if (type.equals("cycleway") || type.equals("path")) {
				infrastructureFactor = 1.00;
			} else if (type.equals("footway") || type.equals("track") || type.equals("pedestrian")) {
				infrastructureFactor = .95;
			} else if (type.equals("steps")) {
				infrastructureFactor = .10;
			}
		} else {
			infrastructureFactor = .85;
		}
		return infrastructureFactor;
	}
}
