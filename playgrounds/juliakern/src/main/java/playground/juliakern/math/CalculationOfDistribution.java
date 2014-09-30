/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.juliakern.math;

public class CalculationOfDistribution {
	
	public static void main(String [] args){
		
		double sum;
		double fxstart = 20000.-125.;
		double fystart = 15000.-125.;

		double rsquared = Math.PI*1000.*1000.;
		
		for(int xshift = 0; xshift<10; xshift++){
			for(int yshift = 0; yshift<10; yshift++){
				sum =0.0;
				double fx = fxstart + xshift * 25;
				double fy = fystart + yshift * 25;
				//rsquared = Math.PI;
				for(int x=1; x<161;x++){
					for(int y=1; y<121; y++){ 
						double xdistance = (fx-250.*x)*(fx-250.*x);
						double ydistance = (fy-250.*y)*(fy-250.*y);
						double distanceSquared = xdistance + ydistance;
						sum += Math.exp(-(distanceSquared/rsquared));
			}
		}
		System.out.println("for link center at " +fx + "," +fy+" the sum is" + sum);
		}
		}
		
		for(int xshift = 0; xshift<10; xshift++){
			for(int yshift = 0; yshift<10; yshift++){
				sum =0.0;
				double fx = fxstart + xshift * 25;
				double fy = fystart + yshift * 25;
				double xdistance = (fx-250.*80)*(fx-250.*80);
				double ydistance = (fy-250.*60)*(fy-250.60);
				double distanceSquared = xdistance + ydistance;
				double weightOflinkForCenterCell = Math.exp(-(distanceSquared/rsquared));
				System.out.println("weight at center cell for linkcenter " + fx +"," + fy + " is " +weightOflinkForCenterCell);
			}
		}
	}

}
