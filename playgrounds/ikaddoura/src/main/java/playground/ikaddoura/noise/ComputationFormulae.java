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

//import org.apache.log4j.Logger;

public class ComputationFormulae {

//	private static final Logger log = Logger.getLogger(ComputationFormulae.class);
	
//	static double annualCostRate = (85.0/(1.95583));
	static double annualCostRate = (85.0/(1.95583))*(Math.pow(1.02, (2014-1995)));
	
	public ComputationFormulae() {
		
	}
	
	public static double calculateDamageCosts(double noiseImmission, double affectedAgentUnits , double timeInterval) {
		String dayOrNight = "NIGHT";
		if(timeInterval>6*3600 && timeInterval<=22*3600) {
			dayOrNight = "DAY";
		}
		
		double lautheitsgewicht = calculateLautheitsgewicht(noiseImmission, dayOrNight);  
		
		double laermEinwohnerGleichwert = lautheitsgewicht*affectedAgentUnits;

//		double damageCosts = 0.;
//		if(dayOrNight == "DAY"){
//			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/(16.0/24.0));
//		}else if(dayOrNight == "NIGHT"){
//			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/(8.0/24.0));
//		}else{
//			throw new RuntimeException("Neither day nor night!");
//		}
//		return damageCosts;
		
//		double damageCosts = 0.;
//		if(dayOrNight == "DAY"){
//			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/((16./24.)/24.0));
//		}else if(dayOrNight == "NIGHT"){
//			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/((8./24.)/24.0));
//		}else{
//			throw new RuntimeException("Neither day nor night!");
//		}
//		return damageCosts;
		
		double damageCosts = 0.;
		if(dayOrNight == "DAY"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		}else if(dayOrNight == "NIGHT"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		}else{
			throw new RuntimeException("Neither day nor night!");
		}
		return damageCosts;	
	}

	public static double calculateLautheitsgewicht (double noiseImmission , String dayOrNight){
		double lautheitsgewicht = 0;
		
		// TODO: Case differentiation for time intervals which overlap the boundaries
		// TODO: investigate, if a fluid boundary instead of a hard boundary can improve the welfare
		
		if(dayOrNight == "DAY"){
			if(noiseImmission<50){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 50));
			}
		}else if(dayOrNight == "NIGHT"){
			if(noiseImmission<40){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 40));
			}
		}else{
			throw new RuntimeException("Neither day nor night!");
		}
		
		return lautheitsgewicht;
		
	}
	
}
