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

import java.util.List;

/**
 * 
 * Contains general equations that are relevant to compute noise immission levels, basically based on the German EWS approach.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseImmissionEquations {
			
	public double calculateResultingNoiseImmission (List<Double> noiseImmissions){
		double resultingNoiseImmission = 0.;
		
		if(noiseImmissions.size() > 0) {
			double sumTmp = 0.;
			for(double noiseImmission : noiseImmissions){
				sumTmp = sumTmp + (Math.pow(10,(0.1*noiseImmission)));
			}
			resultingNoiseImmission = 10 * Math.log10(sumTmp);
			if(resultingNoiseImmission < 0) {
				resultingNoiseImmission = 0.;
			}
		}
		
		return resultingNoiseImmission;
	}
	
	public double calculateShareOfResultingNoiseImmission (double noiseImmission , double resultingNoiseImmission){
		double shareOfResultingNoiseImmission = 0.;
			
		shareOfResultingNoiseImmission = Math.pow(((Math.pow(10, (0.05*noiseImmission)))/(Math.pow(10, (0.05*resultingNoiseImmission)))), 2);
		
		return shareOfResultingNoiseImmission;	
	}
	
}
