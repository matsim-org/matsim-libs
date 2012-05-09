/* *********************************************************************** *
 * project: org.matsim.*
 * EllipseSearch.java
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.pRstrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class EllipseSearch {

	

	public List<PrWeight> getPrWeights(Network net, List<ParkAndRideFacility> prFacilities, Coord homeCoord, Coord workCoord) {
		List <PrWeight> prWeights = new ArrayList<PrWeight>();
		
		for (ParkAndRideFacility pr : prFacilities){
			Id prId = pr.getPrLink3in();
			Coord prCoord = net.getLinks().get(prId).getToNode().getCoord();	
			 
			 double xHomeToPR = Math.abs(homeCoord.getX() - prCoord.getX());
			 double yHomeToPR = Math.abs(homeCoord.getY() - prCoord.getY());
			 double distHomeToPR = getHyp(xHomeToPR, yHomeToPR);
			 
			 double xWorkToPR = Math.abs(workCoord.getX() - prCoord.getX());
			 double yWorkToPR = Math.abs(workCoord.getY() - prCoord.getY());
			 double distWorkToPR = getHyp(xWorkToPR, yWorkToPR);

			 double r = distHomeToPR + distWorkToPR;
			 double weight = 1 / Math.pow(r, 2);
			
			 prWeights.add(new PrWeight(pr.getId(), weight));
		}
		return prWeights;
	}
	
	private double getHyp(double a, double b) {
		double aSquare = Math.pow(a, 2);
		double bSquare = Math.pow(b, 2);
		return Math.sqrt(aSquare + bSquare);
	}
	
	public Link getRndPrLink(Network net, List<PrWeight> prWeights) {

//		System.out.println("unsortedList:");
//		for (PREntry entry : prEntries) {
//			System.out.println("id / value: " + entry.getId() + " / " + entry.getWeight());
//		}

		Collections.sort(prWeights);

//		System.out.println("sortedList:");
//		for (PREntry entry : prEntries) {
//			System.out.println("id / value: " + entry.getId() + " / " + entry.getWeight());
//		}
		
		double weightSum = 0.0;
		for (PrWeight prWeight : prWeights){
			weightSum = weightSum + prWeight.getWeight();
		}
		System.out.println("weightsSum: " + weightSum);

		Random random = new Random();
		double rnd = random.nextDouble() * weightSum;
		System.out.println("rnd: " + rnd);

		Id weightedRndId = null;
		double cumulatedWeight = 0.0;
		for (PrWeight entry : prWeights) {
			cumulatedWeight = cumulatedWeight + entry.getWeight();
			if (cumulatedWeight >= rnd) {
				weightedRndId = entry.getId();
				break;
			}
		}
		System.out.println("weightedRndId: " + weightedRndId.toString());
		
		Link rndPRLink = net.getLinks().get(weightedRndId);
		return rndPRLink;
	}

}
