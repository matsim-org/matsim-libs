/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.christoph.evacuation.router.util.DistanceFuzzyFactorProviderFactory;

public class EvacuationConfig {
	
	private static final Logger log = Logger.getLogger(EvacuationConfig.class);
	
	public static double evacuationTime = 3600 * 8.0;	// time when the incident takes place
	public static double evacuationDelayTime = 3600 * 8.0;	// time until the area should be cleared
	
	/*
	 * Even if meet at home and evacuate afterwards might be more time consuming than
	 * evacuating directly (but not joined), a household might still prefer the first option.
	 * We assume that a certain time overhead compared to the second option is accepted.
	 * E.g. accept 25% time overhead: if t1 < t2  * (1 + 0.25) -> meet at home first.
	 */
	public static double acceptedMeetAtHomeTimeOverhead = 0.25;
	
	public static double innerRadius = 30000.0;
	public static double outerRadius = 30500.0;
	
	/*
	 * If true, pt travel times are estimated using the transit router.
	 * Otherwise, they are estimated based on the matrix used for the
	 * KTI runs.
	 */
	public static boolean useTransitRouter = true;
	public static String transitRouterFile = "";
	
//	public static Coord centerCoord = new CoordImpl("683518.0","246836.0");	// Bellevue Coord
	public static Coord centerCoord = new Coord(Double.parseDouble("640050.0"), Double.parseDouble("246256.0"));	// Coordinates of KKW Goesgen


	public static String dhm25File = "";
	public static String srtmFile = "";
	
	public static String householdObjectAttributesFile = "";
	
	public static List<String> evacuationArea = new ArrayList<String>();
	
	public static List<String> vehicleFleet = new ArrayList<String>();

	public static double affectedAreaTimePenaltyFactor = 1.20;
	public static double affectedAreaDistanceBuffer = 2500.0;

	public static double ptTravelTimePenaltyFactor = Double.MAX_VALUE;
	
	public static double panicShare = 0.00;
	public static double compassProbability = 0.667;
	public static boolean tabuSearch = true;

	public static double duringLegReroutingShare = 1.00;
	
	/*
	 * Several classes use a DeterministicRNG object which produces random numbers
	 * ~ 40% faster than java's default random number generator. Moreover, it is easier
	 * to ensure that always the same random numbers are generated, even if other
	 * pieces of the code have changed.
	 */
	public static long deterministicRNGOffset = 0;
	
	public static long householdsInformerRandomSeed = 132456;
	public static long departureDelayRandomSeed = 123654;
	public static long evacuationDecisionRandomSeed = 471982;
	public static long fuzzyTravelTimeEstimatorRandomSeed = 213456;
	public static long latestAcceptedLeaveTimeRandomSeed = 123984;
	
	/*
	 * This parameter is ignored if EvacuationDecisionBehaviour is not set to SHARE!
	 */
	public static double householdParticipationShare = 1.00;	
	
	public static enum EvacuationDecisionBehaviour {SHARE, MODEL};
	public static EvacuationDecisionBehaviour evacuationDecisionBehaviour = EvacuationDecisionBehaviour.MODEL;
	
	public static enum PickupAgentBehaviour {ALWAYS, NEVER, MODEL};
	public static PickupAgentBehaviour pickupAgents = PickupAgentBehaviour.MODEL;
	
	public static boolean useFuzzyTravelTimes = true;
	
	public static double informAgentsRayleighSigma = 300.0;
	
	public static double capacityFactor = 1.0;
	public static double speedFactor = 1.0;
	
	/*
	 * Analysis modules
	 */
	public static boolean createEvacuationTimePicture = true;
	public static boolean countAgentsInEvacuationArea = true;
	
	/*
	 * survey based model parameter
	 */	
	public static double pickupModelAlwaysConst = 2.67;
	public static double pickupModelAlwaysAge31to60 = -0.71;
	public static double pickupModelAlwaysAge61to70 = -0.71;
	public static double pickupModelAlwaysAge71plus = 6.08;
	public static double pickupModelAlwaysHasChildren = 1.66;
	public static double pickupModelAlwaysHasDrivingLicence = 1.54;
	public static double pickupModelAlwaysIsFemale = -0.65;
	
	public static double pickupModelIfSpaceConst = 0.92;
	public static double pickupModelIfSpaceAge31to60 = -0.81;
	public static double pickupModelIfSpaceAge61to70 = -0.76;
	public static double pickupModelIfSpaceAge71plus = 6.43;
	public static double pickupModelIfSpaceHasChildren = 1.57;
	public static double pickupModelIfSpaceHasDrivingLicence = 3.00;
	public static double pickupModelIfSpaceIsFemale = -0.47;

	/*
	 * These two values are fixed so far. They might be changed in another study.
	 */
	public static enum PreEvacuationTime {TIME0, TIME8, TIME16};
	public static enum EvacuationReason {WATER, FIRE, CHEMICAL, ATOMIC};
	public static PreEvacuationTime leaveModelPreEvacuationTime = PreEvacuationTime.TIME8;
	public static EvacuationReason leaveModelEvacuationReason = EvacuationReason.ATOMIC;
	
	public static double leaveModelHasChildren = 0.60;
	public static double leaveModelHasDrivingLicense = 0.52;
	
	public static double leaveModelImmediatelyConst = 4.10;
	public static double leaveModelImmediatelyChemical = 1.61;
	public static double leaveModelImmediatelyAtomic = 2.08;
	public static double leaveModelImmediatelyFire = 0.59;
	public static double leaveModelImmediatelyAge31to60 = -3.12;
	public static double leaveModelImmediatelyAge61plus = -3.49;
	public static double leaveModelImmediatelyTime8 = -1.66;
	public static double leaveModelImmediatelyHouseholdUnited1 = -0.07;
	public static double leaveModelImmediatelyTime16 = -1.99;
	public static double leaveModelImmediatelyHouseholdUnited2 = -0.33;
	
	public static double leaveModelLaterConst = 3.36;
	public static double leaveModelLaterChemical = 0.982;
	public static double leaveModelLaterAtomic = 0.777;
	public static double leaveModelLaterFire = 0.297;
	public static double leaveModelLaterAge31to60 = -1.9;
	public static double leaveModelLaterAge61plus = -2.13;
	public static double leaveModelLaterTime8 = 0.458;
	public static double leaveModelLaterHouseholdUnited1 = -2.95;
	public static double leaveModelLaterTime16 = 0.275;
	public static double leaveModelLaterHouseholdUnited2 = -1.69;
	
	
	public static Coord getRescueCoord() {
		return new Coord(centerCoord.getX() + 50000.0, centerCoord.getY() + 50000.0);
	}
	
	public static void printConfig() {
		log.info("evacuation start time:\t" + evacuationTime);
		log.info("delay to latest leave time:\t" + evacuationDelayTime);
		log.info("inner radius:\t" + innerRadius);
		log.info("outer radius:\t" + outerRadius);
		log.info("center coordinate:\t" + centerCoord.toString());
		log.info("dhm 25 file:\t" + dhm25File);
		log.info("srtm file:\t" + srtmFile);
		log.info("household object attributes file:\t" + householdObjectAttributesFile);
		
		for (String string : evacuationArea) {
			log.info("evacuation area file:\t" + string);
		}

		for (String string : vehicleFleet) {
			log.info("vehicle fleet file:\t" + string);
		}

		log.info("affected area time penalty factor:\t" + affectedAreaTimePenaltyFactor);
		log.info("affected area distance buffer:\t" + affectedAreaDistanceBuffer);
		log.info("pt travel time penalty factor:\t" + ptTravelTimePenaltyFactor);
		log.info("panic share:\t" + panicShare);
		log.info("compass probability:\t" + compassProbability);
		log.info("tabu search:\t" + tabuSearch);
		log.info("household participation share:\t" + householdParticipationShare);
		log.info("during leg re-routing share:\t" + duringLegReroutingShare);
		log.info("agent pickup behaviour:\t" + pickupAgents.toString());
		log.info("use fuzzy travel times:\t" + useFuzzyTravelTimes);
		log.info("use lookup map for fuzzy travel times:\t" + DistanceFuzzyFactorProviderFactory.useLookupMap);
		log.info("create evacuation time picture:\t" + createEvacuationTimePicture);
		log.info("count agents in evacuation are:\t" + countAgentsInEvacuationArea);
		log.info("sigma for inform-agents Rayleigh function:\t" + informAgentsRayleighSigma);
		log.info("Network capacity factor:\t" + capacityFactor);
		log.info("Network speed factor:\t" + speedFactor);
		log.info("Evacuation decision:\t" + evacuationDecisionBehaviour.toString());
		log.info("use transit router:\t" + useTransitRouter);
		log.info("transit router network file:\t" + transitRouterFile);
		
		log.info("offset for DeterministicRNG instances:\t" + deterministicRNGOffset);
		log.info("random seed for HouseholdsInformer instances:\t" + householdsInformerRandomSeed);
		log.info("random seed for DepartureDelayModel instances:\t" + departureDelayRandomSeed);
		log.info("random seed for FuzzyTravelTimeEstimator instances:\t" + fuzzyTravelTimeEstimatorRandomSeed);
		log.info("random seed for LatestAcceptedLeaveTimeModel instances:\t" + latestAcceptedLeaveTimeRandomSeed);
	}
}
