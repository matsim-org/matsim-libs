/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.accessibility;

/**
 * @author thomas
 *
 */
public class Costs {
	
	public static void main(String args[]){
		
		double gapOrigin2Network = 50.; 	// 50m
		double distanceOnNetwork = 5000.;	// 5000m	
		double travelTimeOnNetwork = 6.;	// 6min
		
		double gapOpportunityA = 61.;		// 61m
		double gapOpportunityB = 40.;		// 40m
		double gapOpportunityC = 13.;		// 13m
		
		double walkTravelTime = 1000.;		// 1km/h = 1000m/h
		
		double betaX = -1.;
		
//		double cikDistanceA = gapOrigin2Network + distanceOnNetwork + gapOpportunityA;
//		double cikDistanceB = gapOrigin2Network + distanceOnNetwork + gapOpportunityB;
//		double cikDistanceC = gapOrigin2Network + distanceOnNetwork + gapOpportunityC;
//		
//		double distanceSum = Math.exp(betaX * cikDistanceA) + Math.exp(betaX * cikDistanceB) + Math.exp(betaX * cikDistanceC);
//		
//		double cikDistanceSum = gapOpportunityA + gapOpportunityB + gapOpportunityC;
//		double cikDistanceAgg = gapOrigin2Network + distanceOnNetwork + cikDistanceSum;
//		
//		double distanceAggSum = 3 * Math.exp(betaX * cikDistanceAgg);
//		
//		System.out.println(distanceSum + " --- " + distanceAggSum);
		
		double cikTimeA = gapOrigin2Network/walkTravelTime + travelTimeOnNetwork + gapOpportunityA / walkTravelTime;
		double cikTimeB = gapOrigin2Network/walkTravelTime + travelTimeOnNetwork + gapOpportunityB / walkTravelTime;
		double ciktimeC = gapOrigin2Network/walkTravelTime + travelTimeOnNetwork + gapOpportunityC / walkTravelTime;
		
		double timeSum = Math.exp(betaX * cikTimeA) + Math.exp(betaX * cikTimeB) + Math.exp(betaX * ciktimeC);
		
		double cjkTimeSum = Math.exp(betaX * (gapOpportunityA / walkTravelTime)) + Math.exp(betaX * (gapOpportunityB / walkTravelTime)) + Math.exp(betaX * (gapOpportunityC / walkTravelTime));
		double cikTimeCom = Math.exp(betaX * (gapOrigin2Network/walkTravelTime + travelTimeOnNetwork));
		
		double timeAggSum = cikTimeCom * cjkTimeSum;
		
		System.out.println(timeSum + " --- " + timeAggSum);
	}

}
