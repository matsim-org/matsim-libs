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
package playground.ikaddoura.noise;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;

/**
 * @author lkroeger
 *
 */

public class NoiseImmissionCalculator {
	
	private static final Logger log = Logger.getLogger(NoiseImmissionCalculator.class);
	
	private SpatialInfo spatialInfo;
	
	public NoiseImmissionCalculator(SpatialInfo spatialInfo){
		this.spatialInfo = spatialInfo;
	}
	
	public double calculateResultingNoiseImmission (List<Double> noiseImmissions){
		double resultingNoiseImmission = 0.;
		
		if(noiseImmissions.size()>0) {
			double sumTmp = 0.;
			for(double noiseImmission : noiseImmissions){
				sumTmp = sumTmp + (Math.pow(10,(0.1*noiseImmission)));
			}
			resultingNoiseImmission = 10 * Math.log10(sumTmp);
			if(resultingNoiseImmission<0) {
				resultingNoiseImmission = 0.;
			}
		}
//		log.info("resultingNoiseImmission: "+resultingNoiseImmission);
		
		return resultingNoiseImmission;
	}
	
	public double calculateShareOfResultingNoiseImmission (double noiseImmission , double resultingNoiseImmission){
		double shareOfResultingNoiseImmission = 0.;
			
		shareOfResultingNoiseImmission = Math.pow(((Math.pow(10, (0.05*noiseImmission)))/(Math.pow(10, (0.05*resultingNoiseImmission)))), 2);
		
		return shareOfResultingNoiseImmission;	
	}
	
	public static List<Double> calculateSharesOfResultingNoiseImmission (List<Double> noiseImmissions , double resultingNoiseImmission){
		List<Double> sharesOfResultingNoiseImmission = new ArrayList<Double>();
			
		for(double noiseImmission : noiseImmissions){
			double share = 0.;
			
			share = Math.pow(((Math.pow(10, (0.05*noiseImmission)))/(Math.pow(10, (0.05*resultingNoiseImmission)))), 2);

			sharesOfResultingNoiseImmission.add(share);
		}
		
		return sharesOfResultingNoiseImmission;	
	}
	
	public double calculateNoiseImmission(Scenario scenario , Id linkId , double NoiseEmission , Coord coord){
		double NoiseImmission = 0.;
		
		Id receiverPointId = spatialInfo.getCoord2receiverPointId().get(coord);
	
		NoiseImmission = NoiseEmission
				+ spatialInfo.getReceiverPointId2RelevantLinkIds2Ds().get(receiverPointId).get(linkId)
				+ spatialInfo.getReceiverPointId2RelevantLinkIds2AngleImmissionCorrection().get(receiverPointId).get(linkId)
				+ spatialInfo.getReceiverPointId2RelevantLinkIds2Drefl().get(receiverPointId).get(linkId)
				+ spatialInfo.getReceiverPointId2RelevantLinkIds2DbmDz().get(receiverPointId).get(linkId)
				;
		
		if(NoiseImmission<0) {
			NoiseImmission = 0.;
		}
		return NoiseImmission;
	}
	
	public double calculateNoiseImmissionParts(Scenario scenario , Id receiverPointId , Id linkId , double noiseEmission , double densityValue, double additionalValue){
		double noiseImmission = 0.;
		double sumTmp = 0.;
		
//		double lengthOfTheParts = spatialInfo.getReceiverPoint2RelevantlinkIds2lengthOfPartOfLinks().get(receiverPointId).get(linkId);
		
		for(int i : spatialInfo.getReceiverPointId2RelevantLinkIds2partOfLinks2distance().get(receiverPointId).get(linkId).keySet()) {
			double noiseImmissionTmp = 0.;
//			double distanceToRoad = spatialInfo.getReceiverPointId2RelevantLinkIds2partOfLinks2distance().get(receiverPointId).get(linkId).get(i);
		
			noiseImmissionTmp = noiseEmission 
					+ spatialInfo.getReceiverPointId2RelevantLinkIds2PartOfLinks2DlParts().get(receiverPointId).get(linkId).get(i)
//					+ calculateDlParts(lengthOfTheParts)
					- spatialInfo.getReceiverPointId2RelevantLinkIds2PartOfLinks2DsParts().get(receiverPointId).get(linkId).get(i)
//					- calculateDsParts(distanceToRoad)
					- spatialInfo.getReceiverPointId2RelevantLinkIds2PartOfLinks2DbmDzParts().get(receiverPointId).get(linkId).get(i)
//					- calculateDbmDzParts(distanceToRoad,densityValue,additionalValue)
//					+ calculateDmeteorologyParts()
					+ spatialInfo.getReceiverPointId2RelevantLinkIds2PartOfLinks2DreflParts().get(receiverPointId).get(linkId).get(i)
//					+ calculateDreflectionParts(scenario , spatialInfo.getReceiverPointId2Coord().get(receiverPointId) , linkId)
					;

			sumTmp = sumTmp + Math.pow(10, 0.1*noiseImmissionTmp);

		}
		
		noiseImmission = 10*Math.log10(sumTmp);

		
		if(noiseImmission<0) {
			noiseImmission = 0.;
		}
		
		return noiseImmission;
	}
	
//	public double calculateDreflectionParts (Scenario scenario , Coord coord , Id linkId) {
//		double Dreflection = 0.;	
//		double densityValue = 0.;
//		if(spatialInfo.getActivityCoords2densityValue().containsKey(coord)) {
//			densityValue = spatialInfo.getActivityCoords2densityValue().get(coord);
//		}
//		
//		double streetWidth = spatialInfo.getLinkId2streetWidth().get(linkId);
//		
//		Dreflection = densityValue/streetWidth;
//		
//		if(Dreflection>3.2) {
//			Dreflection = 3.2;
//		}
//		
//		Dreflection = Dreflection*1.5;
//		
//		// For the consideration of the singualar-reflection-effects,
//		// in dependence of the streetWdith, the height of the buildings
//		// and the structure (in particular the gaps between the buildings),
//		// and also the distance to the emission-source
//		// an additional effect of 0-3 dB(A) ispossible,
//		// effectively much smaller than 3 dB(A).
//		// Therefore the reflection-effect calculated for the multiple reflection effects is multiplied by 1.5
//		
//		// Potential absorbing properties of the buildings are not considered here
//		
//		return Dreflection;
//	}
	
	public double calculateDmeteorologyParts (double distanceToRoad , String DayEveningOrNight) {
		double Dmeteorology = 0.;
		if(distanceToRoad <= 45.) {
			Dmeteorology = 0.;
		} else {
			double factor = 1. - (45./distanceToRoad);
			double C0 = 0.;
			if(DayEveningOrNight.equals("Day")) {
				C0 = 2.;
			} else if(DayEveningOrNight.equals("EVENING")) {
				C0 = 1.;
			}
		}		
		return Dmeteorology;
	}
	
//	public double calculateDsParts (double distanceToRoad){
//		double Ds = 0.;
//		
//		Ds = (20*Math.log10(distanceToRoad)) + (distanceToRoad/200) - 11.2;
//		
//		// TODO: Gebaeudemodell approximieren;
//		
//		return Ds;
//	}
	
//	public double calculateDlParts (double length) {
//		double Dl = 0.;
//		
//		Dl = 10*(Math.log10(length));
//		
//		return Dl;
//	}
	
//	public double calculateDbmDzParts (double distanceToRoad , double densityValue , double additionalValue) {
//		double DbmDz = 0.;
//		
//		double Dbm = calculateDbmParts(distanceToRoad);
//		double Dz = calculateDzParts(distanceToRoad,densityValue,additionalValue);
//		
//		if((Math.abs(Dbm))>(Math.abs(Dz))) {
//			DbmDz = Dbm;
//		} else {
//			DbmDz = Dz;
//		}
//		
//		return DbmDz;
//	}
//	
//	public double calculateDbmParts (double distanceToRoad) {
//		double Dbm = 0.;
//		
//		Dbm = 4.8 - ((2.25/distanceToRoad)*(34 + (600/distanceToRoad)));
//		
//		if(Dbm<0.) {
//			Dbm=0.;
//		}
//		
//		return Dbm;
//	}
//	
//	public double calculateDzParts (double distanceToRoad , double densityValue , double additionalValue) {
//		double Dz = 0.;
//		
//		double z = 0.;
//		
//		if(distanceToRoad<=(30.+additionalValue)) {
//			z = 0.;
//		} else if (distanceToRoad<=(130.+additionalValue)) {
//			z = (Math.sqrt(Math.random())) * ((Math.log10(distanceToRoad-(29.+additionalValue)))/2.) * (4. + 26.*(densityValue/100.));			
//		} else {
//			z = (Math.sqrt(Math.random())) * (4. + 26.*(densityValue/100.));	
//		}
//		
//		// for the correct calculation of the height correction
////		double gamma = 1000.;
////		if(distanceToRoad>125.) {
////			gamma = distanceToRoad*8.;
////		}
//		
//		if(distanceToRoad<(30.+additionalValue)) {
//			Dz = 0.;
//		} else {
//			Dz = 10*(Math.log10(3+(60*z)));
//		}
//				
//		return Dz;
//	}
	
}
