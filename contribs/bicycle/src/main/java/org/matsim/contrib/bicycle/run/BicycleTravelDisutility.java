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
package org.matsim.contrib.bicycle.run;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleLabels;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author smetzler, dziemke
 */
public class BicycleTravelDisutility implements TravelDisutility {
	private static final Logger LOG = Logger.getLogger(BicycleTravelDisutility.class);

	private final double marginalCostOfTime_s;
	private final double marginalCostOfDistance_m;
	private final double marginalCostOfInfrastructure_m;
	private final double marginalCostOfComfort_m;
	private final double marginalCostOfGradient_m_100m;
	private final double sigma;
	private final TravelTime timeCalculator;
	private final Network network;

	private static int normalisationWrnCnt = 0;

	BicycleTravelDisutility(BicycleConfigGroup bicycleConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup,
			PlansCalcRouteConfigGroup plansCalcRouteConfigGroup, TravelTime timeCalculator, Network network) {
		final PlanCalcScoreConfigGroup.ModeParams bicycleParams = cnScoringGroup.getModes().get("bicycle");
		if (bicycleParams == null) {
			throw new NullPointerException("Bicycle is not part of the valid mode parameters " + cnScoringGroup.getModes().keySet());
		}

		this.marginalCostOfDistance_m = -(bicycleParams.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney())
				- bicycleParams.getMarginalUtilityOfDistance();
		this.marginalCostOfTime_s = -(bicycleParams.getMarginalUtilityOfTraveling() / 3600.0) + cnScoringGroup.getPerforming_utils_hr() / 3600.0;

		this.marginalCostOfInfrastructure_m = -(bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m());
		this.marginalCostOfComfort_m = -(bicycleConfigGroup.getMarginalUtilityOfComfort_m());
		this.marginalCostOfGradient_m_100m = -(bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m());

		// Does not seem to be implemented yet
		// this.sigma = plansCalcRouteConfigGroup.getRoutingRandomness();
		this.sigma = 0.2;
		//this.sigma = 0.0;

		this.timeCalculator = timeCalculator;
		
		// TODO only needed as long as network mode filtering kicks out attributes; remove when possible, dz, sep'17
		this.network = network;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		Link linkWithAttributes = network.getLinks().get(link.getId());
		double travelTime = timeCalculator.getLinkTravelTime(linkWithAttributes, time, person, vehicle);
		return getTravelDisutilityBasedOnTTime(linkWithAttributes, time, person, vehicle, travelTime);
	}

	public double getTravelDisutilityBasedOnTTime(Link link, double enterTime, Person person, Vehicle vehicle, double travelTime) {
		String surface = (String) link.getAttributes().getAttribute(BicycleLabels.SURFACE);
		String type = (String) link.getAttributes().getAttribute("type");
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleLabels.CYCLEWAY);

		double distance = link.getLength();
		
		double travelTimeDisutility = marginalCostOfTime_s * travelTime;
		double distanceDisutility = marginalCostOfDistance_m * distance;
		
		double comfortFactor = getComfortFactor(surface, type);
		double comfortDisutility = marginalCostOfComfort_m * (1. - comfortFactor) * distance;
		
		double infrastructureFactor = getInfrastructureFactor(type, cyclewaytype);
		double infrastructureDisutility = marginalCostOfInfrastructure_m * (1. - infrastructureFactor) * distance;
		
		double gradientFactor = getGradientFactor(link);
		double gradientDisutility = marginalCostOfGradient_m_100m * gradientFactor * distance;
		
//		LOG.warn("link = " + link.getId() + "-- travelTime = " + travelTime + " -- distance = " + distance + " -- comfortFactor = "
//				+ comfortFactor	+ " -- infraFactor = "+ infrastructureFactor + " -- gradient = " + gradientFactor);
		 
		// TODO Gender
		// TODO Activity
		// TODO Other influence factors

		 double normalization = 1;
		 if (sigma != 0.) {
			 normalization = 1. / Math.exp(this.sigma * this.sigma / 2);
			 if (normalisationWrnCnt < 10) {
				 normalisationWrnCnt++;
				 LOG.info("Sigma = " + this.sigma + " -- resulting normalization: " + normalization);
			 }
		}
		Random random2 = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible. dz, aug'17
		double logNormalRnd = Math.exp(sigma * random2.nextGaussian());
		logNormalRnd *= normalization;

//		LOG.warn("link = " + link.getId() + " -- travelTimeDisutility = " + travelTimeDisutility + " -- distanceDisutility = "+ distanceDisutility
//				+ " -- infrastructureDisutility = " + infrastructureDisutility + " -- comfortDisutility = "
//				+ comfortDisutility + " -- gradientDisutility = " + gradientDisutility + " -- randomfactor = " + logNormalRnd);
		return (travelTimeDisutility + logNormalRnd * (distanceDisutility + infrastructureDisutility + comfortDisutility + gradientDisutility));
	}

	private double getGradientFactor(Link link) {
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

	// TODO combine this with speeds
	private double getComfortFactor(String surface, String type) {
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

	private double getInfrastructureFactor(String type, String cyclewaytype) {
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

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}