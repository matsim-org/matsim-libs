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
import java.util.ArrayList;


/**
 * @author Matthias Feil
 */
public class UniSAM {

	private static final Logger log = Logger.getLogger(UniSAM.class);
	private final double GWact, GWmode, GWlocation; 
	
	public UniSAM (){
		this.GWact = 2.0;
		this.GWmode = 1.0;
		this.GWlocation = 1.0;
	}
	
	public double run(PlanImpl origPlan, PlanImpl comparePlan){
		
		// Calculate tables per attribute dimension
		// Length is number of acts minus last home plus 0th position, or number of legs respectively
		double [][][] table = new double [3][origPlan.getPlanElements().size()/2+1][comparePlan.getPlanElements().size()/2+1];
		
		for (int k=0;k<table.length;k++){
			double GW = 0;
			if (k==0) GW = this.GWact;
			else if (k==1) GW = this.GWmode;
			else GW = this.GWlocation;
			// Levenshtein distance
			for (int i=0;i<table[k].length;i++){
				for (int j=0;j<table[k][0].length;j++){
					if (j==0){
						// margin orig plan
						table[k][i][j]= i * GW;
					}
					else {
						//margin compare plan
						if (i==0) table[k][i][j]= j * GW;
						else if (k==0) { // Activity type sequence
							table[k][i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getType(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getType(), table[k],i, j, GW);
						}
						else if (k==1){ // Modes
							table[k][i][j] = this.minPath(((LegImpl)(origPlan.getPlanElements().get((i-1)*2+1))).getMode(), ((LegImpl)(comparePlan.getPlanElements().get((j-1)*2+1))).getMode(), table[k],i, j, GW);
						}
						else { // Locations (via linkIDs)
							table[k][i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getLinkId(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getLinkId(), table[k],i, j, GW);
						}
					}
				}
			}
		}
		/*
		// Print tables
		for (int k=0;k<table.length;k++){
			for (int i=0;i<table[k].length;i++){
				for (int j=0;j<table[k][0].length;j++){
					System.out.print(table[k][i][j]+" ");
				}
				System.out.println();
			}
			System.out.println();
		}*/
		
		// Find one optimal trajectory close to the diagonal, for each attribute dimension
		ArrayList<int[]> oset = new ArrayList<int[]>();	// contains the operation and position
		ArrayList<ArrayList<Integer>> dimensions = new ArrayList<ArrayList<Integer>>(); // contains the attribute dimensions of operation and position
		for (int k=0;k<table.length;k++){
			//System.out.println("k = "+k);
			int i=0;
			int j=0;
			boolean goRight = true;
			while (i!=table[k].length-1 || j!=table[k][0].length-1){
				if (i<table[k].length-1 &&
					j<table[k][0].length-1 &&
					table[k][i+1][j+1]==table[k][i][j]){
					//System.out.println("Identity.");
					i++;
					j++;
				}
				// check insertion {1,x}
				else if (j<table[k][0].length-1 &&
						osetContains(oset,dimensions,k,1,j+1)){
					//System.out.println("Insertion.");
					j++;
				}
				// check deletion {2,x}
				else if (i<table[k].length-1 &&
						osetContains(oset,dimensions,k,2,i+1)){
					//System.out.println("Deletion.");
					i++;
				}
				// go new path (insertion)
				else if (goRight && j!=table[k][0].length-1) {
					oset.add(new int[]{1,j+1});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					j++;
					goRight = false;
					//System.out.println("New insertion.");
				}
				// go new path (deletion)
				else {
					oset.add(new int[]{2,i+1});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					i++;
					goRight = true;
					//System.out.println("New deletion.");
				}
			}
		}
		/*
		// Print arrays
		for (int m=0;m<oset.size();m++){
			System.out.print("("+oset.get(m)[0]+","+oset.get(m)[1]+"), ");
			for (int n=0;n<dimensions.get(m).size();n++) System.out.print(dimensions.get(m).get(n)+", ");
			System.out.println();
		}*/
		
		double sum=0;
		for (int m=0;m<oset.size();m++){
			double GW=0;
			if (dimensions.get(m).contains(0)) GW = this.GWact; 
			if (dimensions.get(m).contains(1)) GW = java.lang.Math.max(GW, this.GWmode);
			if (dimensions.get(m).contains(2)) GW = java.lang.Math.max(GW, this.GWlocation);
			sum += GW;
		}
		
		//System.out.println("Sum is "+sum);
		return sum;
	}
	
	private boolean osetContains (ArrayList<int[]> oset, ArrayList<ArrayList<Integer>> dimensions ,int k, int operation, int position){
		for (int m=0;m<oset.size();m++){
			if (oset.get(m)[0]==operation && oset.get(m)[1]==position){
				dimensions.get(m).add(k);
				return true;
			}
		}
		return false;
	}
	
	private double minPath(Object orig, Object compare, double[][]table, int i, int j, double GW){
		double del = table[i-1][j]+GW;
		double ins = table[i][j-1]+GW;
		double sub = Double.MAX_VALUE;
		
		// identity (position-sensitive)
		if (orig.equals(compare)){
			sub =  table[i-1][j-1] + GW/java.lang.Math.max(table.length-1, table[0].length-1)*java.lang.Math.abs(i-j);
		}
		// substitution
		else {
			sub = table[i-1][j-1] + 2 * GW;	
		}
		// return minimum of ins, del, sub
		del = java.lang.Math.min(del, ins);		
		return java.lang.Math.min(del, sub);
	}
}
