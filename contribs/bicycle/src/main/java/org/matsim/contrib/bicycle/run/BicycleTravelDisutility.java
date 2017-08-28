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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

/**
 * TODO revise below javadoc
 * in this class disutility per link is calculateted for routing depending on the following parameters:
 * traveltime, distance, surface, slope/elevation, cyclewaytype, highwaytype (streets with cycleways are prefered)
 * 
 * following parameters may be added in the future
 * smoothness? (vs surface), weather/wind?, #crossings (info in nodes), on-street-parking cars?, prefere routes that are offical bike routes
 * 
 * @author smetzler, dziemke
 */
public class BicycleTravelDisutility implements TravelDisutility {
	private static final Logger LOG = Logger.getLogger(BicycleTravelDisutility.class);

	private final ObjectAttributes bicycleAttributes;
	private final double marginalUtilityOfTime_s;
	private final double marginalUtilityOfDistance_m;
	private final double marginalUtilityOfStreettype_m;
	private final double marginalUtilityOfSurfacetype_m;
	private final double sigma;	
	private final TravelTime timeCalculator;
	
	private static int normalisationWrnCnt = 0;

	
	BicycleTravelDisutility(BicycleConfigGroup bicycleConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup, PlansCalcRouteConfigGroup plansCalcRouteConfigGroup, TravelTime timeCalculator) {
		// Get infos from ObjectAttributes
		bicycleAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(bicycleAttributes).readFile(bicycleConfigGroup.getNetworkAttFile());

		this.marginalUtilityOfDistance_m = cnScoringGroup.getModes().get("bicycle").getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney()
				+ cnScoringGroup.getModes().get("bicycle").getMarginalUtilityOfDistance();
		
		this.marginalUtilityOfTime_s = cnScoringGroup.getModes().get("bicycle").getMarginalUtilityOfTraveling() / 3600.0
				- cnScoringGroup.getPerforming_utils_hr() / 3600.0;

		this.marginalUtilityOfStreettype_m = bicycleConfigGroup.getMarginalUtilityOfStreettype();
		this.marginalUtilityOfSurfacetype_m = bicycleConfigGroup.getMarginalUtilityOfSurfacetype();
		
		this.sigma = plansCalcRouteConfigGroup.getRoutingRandomness();
		
		this.timeCalculator = timeCalculator;
	}

	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {

		double travelTime = timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		return getTravelDisutilityBasedOnTTime(link, time, person, vehicle, travelTime);
	}
	
	
	public double getTravelDisutilityBasedOnTTime(Link link, double enterTime, Person person, Vehicle vehicle, double travelTime) {
		String surface = (String) bicycleAttributes.getAttribute(link.getId().toString(), "surface");
		String highway = (String) bicycleAttributes.getAttribute(link.getId().toString(), "highway");
		String cyclewaytype = (String) bicycleAttributes.getAttribute(link.getId().toString(), "cyclewaytype");

		// distance
		double distance = link.getLength();
		
		// Surface
		double comfortFactor = getComfortFactor(surface, highway);
		
		// Road type
		double infrastructureFactor = getInfrastructureFactor(highway, cyclewaytype);

		// Slope
		// TODO add disutility for slope here, makes sense for hilly cities, but left aside for Berlin, ...says Simon
		//
		// From "Flügel et al. -- Empirical speed models for cycling in the Oslo road network" (not yet published!)
		// Positive gradients (uphill): Roughly linear decrease in speed with increasing gradient
		// At 9% gradient, cyclists are 42.7% slower
		// negative gradients (downhill): 
		// Not linear; highest speeds at 5% or 6% gradient
		// At gradients higher than 6% braking
		
		// TODO Gender
		
		// TODO Activity
		
		// TODO Other influence factors
		
		
		// New randomization
		// yyyyyy in the randomized toll disutility this is LOGnormal.  Should be made consistent, or an argument provided why in the different cases different
		// mathematical forms make sense.  kai, feb'17
		// This following should address this comment, dz, aug'17
//		double normalization = 1;
//		if (sigma != 0.) {
//			normalization = 1. / Math.exp(this.sigma * this.sigma / 2);
//			if (normalisationWrnCnt < 10) {
//				normalisationWrnCnt++;
//				LOG.info("Sigma = " + this.sigma + " -- resulting normalization: " + normalization);
//			}
//		}
//		Random random2 = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible. dz, aug'17
//		double logNormalRnd = Math.exp(sigma * random2.nextGaussian());
//		logNormalRnd *= normalization;
		//
		
		// Old randomization from Simon
		 Random random = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible. dz, aug'17
		 double standardDeviation = 0.2;
		 int mean = 1;
		 double randomfactor = random.nextGaussian() * standardDeviation + mean;
		//
		
		double travelTimeDisutility = -(marginalUtilityOfTime_s * travelTime);
		double distanceDisutility = -(marginalUtilityOfDistance_m * distance);

		// Simon: (Math.pow((1/surfaceFactor), 2) + Math.pow((1/streetFactor), 2)); //TODO vielleicht quadratisch?
		double comfortDisutility = -(marginalUtilityOfSurfacetype_m * (100 - comfortFactor) / 100) * distance;
		
		// Simon: (Math.pow((1/surfaceFactor), 2) + Math.pow((1/streetFactor), 2)); //TODO vielleicht quadratisch?
		double infrastructureDisutility = -(marginalUtilityOfStreettype_m  * (100 - infrastructureFactor) / 100) * distance;
		
		
		// Old disutility
		LOG.info("travelTimeDisutility = " + travelTimeDisutility + " -- distanceDisutility = " + distanceDisutility + " -- streettypeDisutility = "
				+ infrastructureDisutility + " -- surfaceDisutility = " + comfortDisutility + " -- randomfactor = " + randomfactor);
		return (travelTimeDisutility + distanceDisutility + infrastructureDisutility + comfortDisutility) * randomfactor;
		// Example from RandomizingTimeDistanceTravelDisutility: return this.marginalCostOfTime * travelTime + logNormalRnd * this.marginalCostOfDistance * link.getLength();

		
		// Intermediate solution
//		return travelTimeDisutility + randomfactor * (distanceDisutility + infrastructureDisutility + comfortDisutility);

		
		// New disutility
//		LOG.info("travelTimeDisutility = " + travelTimeDisutility + " -- distanceDisutility = " + distanceDisutility + " -- streettypeDisutility = "
//				+ infrastructureDisutility + " -- surfaceDisutility = " + comfortDisutility + " -- randomfactor = " + logNormalRnd);
//		return travelTimeDisutility + logNormalRnd * (distanceDisutility + infrastructureDisutility + comfortDisutility);		
	}
	
	
	private double getComfortFactor(String surface, String highway) {
		double comfortFactor = 100;
		if (surface != null) {
			switch (surface) {
			case "paved": 					comfortFactor= 100; break;
			case "asphalt": 				comfortFactor= 100; break;
			case "cobblestone":				comfortFactor=  40; break;
			case "cobblestone (bad)":		comfortFactor=  30; break;
			case "sett":					comfortFactor=  50; break;
			case "cobblestone;flattened":
			case "cobblestone:flattened": 	comfortFactor=  50; break;

			case "concrete": 				comfortFactor= 100; break;
			case "concrete:lanes": 			comfortFactor=  95; break;
			case "concrete_plates":
			case "concrete:plates": 		comfortFactor=  90; break;
			case "paving_stones": 			comfortFactor=  80; break;
			case "paving_stones:35": 
			case "paving_stones:30": 		comfortFactor=  80; break;

			case "unpaved": 				comfortFactor=  60; break;
			case "compacted": 				comfortFactor=  70; break;
			case "dirt": 					comfortFactor=  30; break;
			case "earth": 					comfortFactor=  30; break;
			case "fine_gravel": 			comfortFactor=  90; break;

			case "gravel": 					comfortFactor=  60; break;
			case "ground": 					comfortFactor=  60; break;
			case "wood": 					comfortFactor=  30; break;
			case "pebblestone": 			comfortFactor=  30; break;
			case "sand": 					comfortFactor=  30; break;

			case "bricks": 					comfortFactor=  60; break;
			case "stone": 					comfortFactor=  40; break;
			case "grass": 					comfortFactor=  40; break;

			case "compressed": 				comfortFactor=  40; break;
			case "asphalt;paving_stones:35":comfortFactor=  60; break;
			case "paving_stones:3": 		comfortFactor=  40; break;
			
			default: 						comfortFactor=  85;
			}
		}
		else {
			// For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
			// For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg
			if (highway != null) {
				if (highway.equals("primary") || highway.equals("primary_link") ||highway.equals("secondary") || highway.equals("secondary_link")) {
					comfortFactor= 100;
				}
				else {
					comfortFactor = 85;
				}
			}
		}
		return comfortFactor;
	}

	
	private double getInfrastructureFactor(String highway, String cyclewaytype) {
		// How safe and comfortable does one feel on this kind of street?
		// Big roads without cycleways are bad, residential roads and footways are okay.
		// Cycle lanes and tracks are good
		// Cycleways are good
		double infrastructureFactor = 100;
		if (highway != null) {
			if (highway.equals("trunk")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = 5;
				} else { // Some kind of cycleway
					infrastructureFactor = 95;
				}
			} else if (highway.equals("primary") || highway.equals("primary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = 10;
				} else { // Some kind of cycleway
					infrastructureFactor = 95;
				}
			} else if (highway.equals("secondary") || highway.equals("secondary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = 30;
				} else { // Some kind of cycleway
					infrastructureFactor = 95;
				}
			} else if (highway.equals("tertiary") || highway.equals("tertiary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = 40;
				} else { // Some kind of cycleway
					infrastructureFactor = 95;
				}
			} else if (highway.equals("unclassified")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = 90;
				} else { // Some kind of cycleway
					infrastructureFactor = 95;
				}
			} else if (highway.equals("unclassified")) {
				infrastructureFactor = 95;
			} else if (highway.equals("service") || highway.equals("living_street") || highway.equals("minor")) {
				infrastructureFactor = 95;
			} else if (highway.equals("cycleway") || highway.equals("path")) {
				infrastructureFactor = 100;
			} else if (highway.equals("footway") || highway.equals("track") || highway.equals("pedestrian")) {
				infrastructureFactor = 95;
			} else if (highway.equals("steps")) {
				infrastructureFactor = 10;
			}
		} else {
			infrastructureFactor = 85;
		}
		return infrastructureFactor;
	}
	
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}