/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * 
 * Collects the relevant information in order to compute the noise immission for each receiver point.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseImmissionCalculation {

	private static final Logger log = Logger.getLogger(NoiseImmissionCalculation.class);
	
	private NoiseInitialization spatialInfo;
	private NoiseParameters noiseParams;
	
	private Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints;
	private NoiseEmissionHandler noiseEmissionHandler;
	
	// optional information for a more detailed calculation of noise immissions
	private final List<Id<Link>> tunnelLinks;
	private final List<Id<Link>> noiseBarrierLinks;
		
	public NoiseImmissionCalculation (NoiseInitialization spatialInfo, NoiseEmissionHandler noiseEmissionHandler, NoiseParameters noiseParams, Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints) {
		this.spatialInfo = spatialInfo;
		this.noiseParams = noiseParams;
		this.receiverPoints = receiverPoints;
		this.noiseEmissionHandler = noiseEmissionHandler;
		
		this.tunnelLinks = noiseParams.getTunnelLinkIDs();
		this.noiseBarrierLinks = null;
		
		if (tunnelLinks == null) {
			log.warn("No information on tunnels provided.");
		}
		
		if (noiseBarrierLinks == null) {
			log.warn("No information on noise barriers provided.");
		}
		
	}
	
	public void calculateNoiseImmission() {
		
		calculateImmissionSharesPerLink();
		calculateFinalNoiseImmissions();
	}

	private void calculateImmissionSharesPerLink() {
		
		log.info("Calculating noise immission shares per link...");
		int counter = 0;
		
		for (ReceiverPoint rp : this.receiverPoints.values()) {
			
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
			
			Map<Double,Map<Id<Link>,Double>> timeIntervals2noiseLinks2isolatedImmission = new HashMap<Double, Map<Id<Link>,Double>>();
		
			for (double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()) {
			 	Map<Id<Link>,Double> noiseLinks2isolatedImmission = new HashMap<Id<Link>, Double>();
			
			 	for(Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
			 		if (tunnelLinks.contains(linkId)) {
			 			// the immission resulting from this link is zero
						noiseLinks2isolatedImmission.put(linkId, 0.);
			 			
			 		} else {
			 			double noiseEmission = noiseEmissionHandler.getLinkId2timeInterval2noiseEmission().get(linkId).get(timeInterval);
						double noiseImmission = 0.;
						if (!(noiseEmission == 0.)) {
							noiseImmission = emission2immission(this.spatialInfo , linkId , noiseEmission , rp.getId());						
						}
						noiseLinks2isolatedImmission.put(linkId,noiseImmission);
			 		}
				}
				timeIntervals2noiseLinks2isolatedImmission.put(timeInterval, noiseLinks2isolatedImmission);
			}
			rp.setTimeInterval2LinkId2IsolatedImmission(timeIntervals2noiseLinks2isolatedImmission);
			counter ++;
		}
		log.info("Calculating noise immission shares per link... Done.");
	}
	
	
	private double emission2immission(NoiseInitialization spatialInfo, Id<Link> linkId, double noiseEmission, Id<ReceiverPoint> rpId) {
		double noiseImmission = 0.;
			
		noiseImmission = noiseEmission
				+ this.receiverPoints.get(rpId).getLinkId2distanceCorrection().get(linkId)
				+ this.receiverPoints.get(rpId).getLinkId2angleCorrection().get(linkId);
		
		if (noiseImmission < 0.) {
			noiseImmission = 0.;
		}
		return noiseImmission;
	}

	private void calculateFinalNoiseImmissions() {
		
		log.info("Calculating final noise immissions...");
		int counter = 0;
		
		for (ReceiverPoint rp : this.receiverPoints.values()) {
			
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
			
			Map<Double,Double> timeInterval2noiseImmission = new HashMap<Double, Double>();
			
			for (double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()) {
				List<Double> noiseImmissions = new ArrayList<Double>();
				
				if (!(rp.getTimeInterval2LinkId2IsolatedImmission().get(timeInterval) == null)) {
				
					for (Id<Link> linkId : rp.getTimeInterval2LinkId2IsolatedImmission().get(timeInterval).keySet()) {
						
						if (!(noiseEmissionHandler.getLinkId2timeInterval2linkEnterVehicleIDs().get(linkId).get(timeInterval).size() == 0.)) {
							noiseImmissions.add(rp.getTimeInterval2LinkId2IsolatedImmission().get(timeInterval).get(linkId));
						}
					}	
					double resultingNoiseImmission = NoiseEquations.calculateResultingNoiseImmission(noiseImmissions);
					timeInterval2noiseImmission.put(timeInterval, resultingNoiseImmission);
					
				} else {
					// if no link has to to be considered for the calculation due to too long distances
					timeInterval2noiseImmission.put(timeInterval, 0.);
				}
			}
			rp.setTimeInterval2immission(timeInterval2noiseImmission);
			counter ++;
		}

		log.info("Calculating final noise immissions... Done.");
	}
	
}
