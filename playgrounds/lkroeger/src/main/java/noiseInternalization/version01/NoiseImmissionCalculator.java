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
package noiseInternalization.version01;

import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;

public class NoiseImmissionCalculator {
	
//	private static final Logger log = Logger.getLogger(NoiseImmissionCalculator.class);

	public NoiseImmissionCalculator(){
		
	}
	
	public static double calculateResultingNoiseImmission (List<Double> noiseImmissions){
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
		return resultingNoiseImmission;
	}
	
	public static double calculateShareOfResultingNoiseImmission (double noiseImmission , double resultingNoiseImmission){
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
	
	public static double calculateNoiseImmission(Scenario scenario , Id linkId , double distanceToRoad , double NoiseEmission , Coord coord , double coordX , double coordY , double relevantFromNodeCoordX , double relevantFromNodeCoordY , double relevantToNodeCoordX , double relevantToNodeCoordY){
		double NoiseImmission = 0.;
		NoiseImmission = NoiseEmission + calculateDs(distanceToRoad) + GetAngle.getImmissionCorrection(coordX, coordY, relevantFromNodeCoordX, relevantFromNodeCoordY, relevantToNodeCoordX, relevantToNodeCoordY) + calculateDreflection(scenario, coord, linkId) + calculateDbmDz(scenario, distanceToRoad, coord);
		if(NoiseImmission<0) {
			NoiseImmission = 0.;
		}
		return NoiseImmission;
	}
	
	public static double calculateNoiseImmission2(Scenario scenario , Id receiverPointId , Id linkId , double noiseEmission){
		double noiseImmission = 0.;
		double sumTmp = 0.;
		
		for(int i : GetNearestReceiverPoint.receiverPointId2RelevantLinkIds2partOfLinks2distance.get(receiverPointId).get(linkId).keySet()) {
			double noiseImmissionTmp = 0.;
			noiseImmissionTmp = noiseEmission + calculateDl2(GetNearestReceiverPoint.receiverPoint2RelevantlinkIds2lengthOfPartOfLinks.get(receiverPointId).get(linkId)) - calculateDs2(GetNearestReceiverPoint.receiverPointId2RelevantLinkIds2partOfLinks2distance.get(receiverPointId).get(linkId).get(i));
			sumTmp = sumTmp + Math.pow(10, 0.1*noiseImmissionTmp); 
		}
		
		noiseImmission = 10*Math.log10(sumTmp);
		
		if(noiseImmission<0) {
			noiseImmission = 0.;
		}
		
		return noiseImmission;
	}
	
	static int x = 0;
	public static double calculateDreflection (Scenario scenario , Coord coord , Id linkId) {
		double Dreflection = 0.;
		
		if(!(GetActivityCoords.activityCoords2densityValue.containsKey(coord))) {
//			System.out.println(coord+"  "+GetActivityCoords.activityCoords2densityValue);
//			System.out.println("coord: "+coord);
		}
		
		double densityValue = 0.;
		
		if(GetActivityCoords.activityCoords2densityValue.containsKey(coord)) {
			densityValue = GetActivityCoords.activityCoords2densityValue.get(coord);
		}
		double streetWidth = GetActivityCoords.linkId2streetWidth.get(linkId);
		
		Dreflection = densityValue/streetWidth;
		
		if(Dreflection>3.2) {
			Dreflection = 3.2;
		}
		
		return Dreflection;
	}
	
	public static double calculateDbmDz (Scenario scenario , double distanceToRoad , Coord coord) {
		double DbmDz = 0.;
		if(distanceToRoad==0.) {
			distanceToRoad = 0.00000001;
			// dividing by zero is not possible
		}
		
		double densityValue = 0.;
		
		if(GetActivityCoords.activityCoords2densityValue.containsKey(coord)) {
			densityValue = GetActivityCoords.activityCoords2densityValue.get(coord);
		}
		
		// D_BM is relevant if there are no buildings which provoke shielding effects
		// The height is chosen to be dependent from the activity locations density
//		double Dbm = -4.8* Math.exp((-1)*(Math.pow((10*(densityValue*0.01)/distanceToRoad)*(8.5+(100/distanceToRoad)),1.3)));
		double Dbm = -4.8* Math.exp((-1)*(Math.pow((((2+(densityValue*0.1)/distanceToRoad)*(8.5+(100/distanceToRoad)))),1.3)));
		
		double Dz = 0.;
		double z = (distanceToRoad/3)*(densityValue*0.01)/100;
		z = z - 1./30.;
		Dz = -10*Math.log10(3+60*z);
		
		if((Math.abs(Dbm))>(Math.abs(Dz))) {
			DbmDz = Dbm;
		} else {
			DbmDz = Dz;	
		}
		return DbmDz;
	}
	
	public static double calculateDs (double distanceToRoad){
		double Ds = 0.;
		
		Ds = 15.8 - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
//		Ds = 15.8 - (12*(distanceToRoad/400)) - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
//		Ds = 15.8 + 2 - (12*(distanceToRoad/400)) - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
		
		// TODO: Gebaeudemodell approximieren;
		
		return Ds;
	}
	
	public static double calculateDs2 (double distanceToRoad){
		double Ds = 0.;
		
		Ds = (20*Math.log10(distanceToRoad)) + (distanceToRoad/200) - 11.2;
		
		// TODO: Gebaeudemodell approximieren;
		
		return Ds;
	}
	
	public static double calculateDl2 (double length) {
		double Dl = 0.;
		
		Dl = 10*Math.log10(length);
		
		return Dl;
	}
	
}
