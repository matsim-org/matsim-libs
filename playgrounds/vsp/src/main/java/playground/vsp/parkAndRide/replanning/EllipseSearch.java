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
package playground.vsp.parkAndRide.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;

import playground.vsp.parkAndRide.PRFacility;

/**
 * @author ikaddoura
 *
 */
public class EllipseSearch {
	private static final Logger log = Logger.getLogger(EllipseSearch.class);

	public List<PRWeight> getPrWeights(int nrPrFacilities, Network net, Map<Id<PRFacility>, PRFacility> id2prFacility, Coord homeCoord, Coord workCoord, double gravity) {
		
		List <PRWeight> prWeights = new ArrayList<PRWeight>();
		
		if (homeCoord == null || workCoord == null){
			throw new RuntimeException("Plan doesn't have home or work activity. Aborting...");
		}
		
		if (nrPrFacilities == 0) {
			log.info("Getting weights for all park-and-ride facilities...");
			for (PRFacility pr : id2prFacility.values()) {
				Id<Link> prLinkId = pr.getPrLink3in();
				Coord prCoord = net.getLinks().get(prLinkId).getToNode().getCoord();
				double weight = calculateWeight(homeCoord, workCoord, prCoord, gravity);
				prWeights.add(new PRWeight(pr.getId(), weight));
			}
		} else {
			
			if (nrPrFacilities > id2prFacility.size()){
				throw new RuntimeException("Number of park-and-ride facilities to calculate the weight for is higher than number of available park-and-ride facilities.");
			}
			
			log.info("Getting weights for " + nrPrFacilities + " randomly chosen park-and-ride facilities...");
			Collection<PRFacility> prFacilities = id2prFacility.values();
			List<PRFacility> prFacilityList = new ArrayList<PRFacility>();
			prFacilityList.addAll(prFacilities);

			List<Id<PRFacility>> insertedPrIds = new ArrayList<>();
			for (int n = 0; n < nrPrFacilities; ){
			
				int rndKey = (int) (MatsimRandom.getRandom().nextDouble() * prFacilities.size());
				PRFacility pr = prFacilityList.get(rndKey);
				
				if (insertedPrIds.contains(pr.getId())){
					log.info("Weight for ParkAndRide Facility " + pr.getId().toString() + " already calculated. Chosing again... ");
				} else {
					log.info("Calculating weight for ParkAndRide Facility " + pr.getId().toString() + "...");					

					Id<Link> prLinkId = pr.getPrLink3in();
					Coord prCoord = net.getLinks().get(prLinkId).getToNode().getCoord();
					double weight = calculateWeight(homeCoord, workCoord, prCoord, gravity);
					
					prWeights.add(new PRWeight(pr.getId(), weight));
					insertedPrIds.add(pr.getId());
					n++;
				}
			}
		}
		return prWeights;
	}

	private double calculateWeight(Coord homeCoord, Coord workCoord, Coord prCoord, double gravity) {

		double xHomeToPR = Math.abs(homeCoord.getX() - prCoord.getX());
		double yHomeToPR = Math.abs(homeCoord.getY() - prCoord.getY());
		double distHomeToPR = getHyp(xHomeToPR, yHomeToPR);

		double xWorkToPR = Math.abs(workCoord.getX() - prCoord.getX());
		double yWorkToPR = Math.abs(workCoord.getY() - prCoord.getY());
		double distWorkToPR = getHyp(xWorkToPR, yWorkToPR);

		double r = (distHomeToPR + distWorkToPR) / 1000.0;
//		System.out.println("home-->P+R-->work: " + homeCoord.toString() + " --> " + prCoord.toString() + " --> " + workCoord.toString() + " = distance [km]: " + r);
		double weight = 1 / Math.pow(r, gravity);
//		System.out.println("calculated weight: "+ weight);

		return weight;
	}

	private double getHyp(double a, double b) {
		double aSquare = Math.pow(a, 2);
		double bSquare = Math.pow(b, 2);
		return Math.sqrt(aSquare + bSquare);
	}
	
	public Link getRndPrLink(Network net, Map<Id<PRFacility>, PRFacility> id2prFacility, List<PRWeight> prWeights) {
		if (prWeights.isEmpty()){
			throw new RuntimeException("For no park-and-ride facility a weight is calculated. Aborting...");
		}
		Collections.sort(prWeights);

//		for (PrWeight prWeight : prWeights) {
//			System.out.println("id / value: " + prWeight.getId() + " / " + prWeight.getWeight());
//		}
		
		double weightSum = 0.0;
		for (PRWeight prWeight : prWeights){
			weightSum = weightSum + prWeight.getWeight();
		}
		
//		for (PrWeight prWeight : prWeights) {
//			System.out.println("id / weight / probability: " + prWeight.getId() + " / " + prWeight.getWeight() + " / " + prWeight.getWeight() / weightSum);
//		}

		double rnd = MatsimRandom.getRandom().nextDouble() * weightSum;

		Id<PRFacility> chosenPrId = null;
		double cumulatedWeight = 0.0;
		for (PRWeight entry : prWeights) {
			cumulatedWeight = cumulatedWeight + entry.getWeight();
			if (cumulatedWeight >= rnd) {
				chosenPrId = entry.getId();
				break;
			}
		}
		log.info("Chosen park-and-ride facility ID: " + chosenPrId.toString());
		Link rndPRLink = net.getLinks().get(id2prFacility.get(chosenPrId).getPrLink3in());
		return rndPRLink;
	}

}
