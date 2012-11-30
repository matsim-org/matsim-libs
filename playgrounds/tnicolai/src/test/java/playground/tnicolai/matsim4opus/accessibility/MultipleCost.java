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
public class MultipleCost {

	/**
	 * @param args
	 * 
	 * 						  cjk1	
	 * 						 /
	 * 						/
	 * 		i--------------j--cjk2
	 * 					    \
	 * 						 \
	 *						  cjk3	 
	 * 
	 * 
	 */
	public static void main(String[] args) {
		
		double betaWalkTT = -2.;
		double betaWalkTD = -1.;
		
		// travel time costs (min)
		double cijTT = 10.; 	// time to reach j from i
		double cjk1TT= 5.;		// time to reach cjk1 from j
		double cjk2TT= 2.;		// time to reach cjk2 from j
		double cjk3TT= 4.;		// time to reach cjk3 from j
		double cik1TT= cijTT + cjk1TT;
		double cik2TT= cijTT + cjk2TT;
		double cik3TT= cijTT + cjk3TT;
		
		// travel distance costs (m)
		double cijTD = 500.;	// distance to reach j from i
		double cjk1TD= 80.;	// distance to reach cjk1 from j
		double cjk2TD= 30.;		// distance to reach cjk2 from j
		double cjk3TD= 50.;		// distance to reach cjk3 from j
		double cik1TD= cijTD + cjk1TD;
		double cik2TD= cijTD + cjk2TD;
		double cik3TD= cijTD + cjk3TD;
		
		// Logsum
		double Vik1 = betaWalkTT * cik1TT + betaWalkTD * cik1TD;
		double Vik2 = betaWalkTT * cik2TT + betaWalkTD * cik2TD;
		double Vik3 = betaWalkTT * cik3TT + betaWalkTD * cik3TD;
		
		double Ai = Math.log( Math.exp(Vik1) + Math.exp(Vik2) + Math.exp(Vik3));
		System.out.println(Ai);
		
		// transformed Logsum
		double Vij = betaWalkTT * cijTT + betaWalkTD * cijTD;
		double Vjk1= betaWalkTT * cjk1TT + betaWalkTD * cjk1TD;
		double Vjk2= betaWalkTT * cjk2TT + betaWalkTD * cjk2TD;
		double Vjk3= betaWalkTT * cjk3TT + betaWalkTD * cjk3TD;
		double Sumjk= Math.exp(Vjk1) + Math.exp(Vjk2) + Math.exp(Vjk3);
		
		double Ai2= Math.log( Math.exp(Vij) * Sumjk);
		System.out.println(Ai2);
		
	}

}
