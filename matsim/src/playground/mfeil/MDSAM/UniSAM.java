/* *********************************************************************** *
 * project: org.matsim.*
 * UniSAM.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;

import org.apache.log4j.Logger;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;


/**
 * @author Matthias Feil
 */
public class UniSAM {

	private static final Logger log = Logger.getLogger(UniSAM.class);
	private final double GW; 
	
	public UniSAM (){
		this.GW = 1.0;
	}
	
	public double run(PlanImpl origPlan, PlanImpl comparePlan){
		
		// Initialize table (only for act types)
		// Length is number of acts minus last home plus 0th position
		double [][] table = new double [origPlan.getPlanElements().size()/2+1][comparePlan.getPlanElements().size()/2+1];
		
		// Levenshtein distance
		for (int i=0;i<table.length;i++){
			for (int j=0;j<table[0].length;j++){
				if (j==0){
					// margin orig plan
					table[i][j]= i * this.GW;
				}
				else {
					//margin compare plan
					if (i==0) table[i][j]= j * this.GW;
					else table[i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getType(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getType(), table, i, j);
					
				}
			}
		}		
	/*	for (int i=0;i<table.length;i++){
			for (int j=0;j<table[0].length;j++){
				System.out.print(table[i][j]+" ");
			}
			System.out.println();
		}*/
		return table[table.length-1][table[0].length-1];
	}
	
	private double minPath(Object orig, Object compare, double[][]table, int i, int j){
		double del = table[i-1][j]+this.GW;
		double ins = table[i][j-1]+this.GW;
		double sub = Double.MAX_VALUE;
		
		// identity (position-sensitive)
		if (orig.equals(compare)){
			sub =  table[i-1][j-1] + this.GW/java.lang.Math.max(table.length-1, table[0].length-1)*java.lang.Math.abs(i-j);
		}
		// substitution
		else {
			sub = table[i-1][j-1] + 2 * this.GW;	
		}
		// return minimum of ins, del, sub
		del = java.lang.Math.min(del, ins);		
		return java.lang.Math.min(del, sub);
	}
}
