/* *********************************************************************** *
 * project: org.matsim.*
 * ReadEvents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.wisinee.IPF;

//============================================================================
//IPF calculation for 2-dimension matrix
//the marginals must be transformed into 2 dimensions before starting calculation
//
//For more information, contact wisinee@hotmail.com
//This code is adapted from the code by Paul Norman, University of Leeds, UK.
//============================================================================
public class Ipf {
	double[] fixedR;
	double[] fixedC;
	double[][]initialRij;
	double[][]finalRij;
	
	public void setFixRow(double[] r,int nr){
		fixedR = r.clone(); 
	}
	public void setFixColumn(double[] c,int nc){
		fixedC = c.clone(); 
	}
	public void setInitialMatrix(double[][] rij,int nr, int nc){
		initialRij = rij.clone(); 
	}
	public double[][] ipfcal(int nr, int nc, double convSet, int maxItn){
		int k = 1, k1 = 0;
		double conv = 0;
		double diff = 0;
				
		double[] totalR = new double[nr];
		double[] totalC = new double[nc];
		double[][] rij = new double[nr][nc];
		
		
		//initialize variables
		for (int i = 0; i < nr; i++) {
			for (int j = 0; j < nc; j++){
				rij[i][j] = initialRij[i][j];
			}
		}	
		initialRij = null;

		double[][] cij = new double[nr][nc];		
		
		//start ipf calculation
		do {
			totalR = new double[nr];		
			for (int i = 0; i < nr; i++){
				for (int j = 0; j < nc; j++) {
					totalR[i] = totalR[i] + rij[i][j];
				}
			}	

			for (int i = 0; i < nr; i++){
				for (int j = 0; j < nc; j++) {
					if (totalR[i]>0) rij[i][j] = rij[i][j]*fixedR[i]/totalR[i];
					else rij[i][j] = 0;
				}			
			}			
		
			for (int j = 0; j < nc; j++){
				for (int i = 0; i < nr; i++) {
					cij[i][j] = rij[i][j];
				}	
			}	
					
			totalC = new double[nc];
			for (int j = 0; j < nc; j++){
				for (int i = 0; i < nr; i++) {
					totalC[j] = totalC[j] + cij[i][j];
				}	
			}	
			
			for (int j = 0; j < nc; j++){
				for (int i = 0; i < nr; i++) {
					if (totalC[j] > 0)  cij[i][j] = cij[i][j]*fixedC[j]/totalC[j];
					else cij[i][j] = 0;
				}			
			}	

			conv = 0;
			for (int i = 0; i < nr; i++){
				for (int j = 0; j < nc; j++) {
					diff = Math.abs(cij[i][j]-rij[i][j]);
					if (diff >= conv){
						conv = diff;
					}	
				}			
			}		
			
			if (conv <= convSet){
				k1 = k;
				break;			
			}
			for (int i = 0; i < nr; i++){
				for (int j = 0; j < nc; j++) {
					rij[i][j] = cij[i][j];
				}			
			}	
			System.out.print("IPF Iteration: " + k + '\t');
			System.out.println("Convergence: " + conv);
			k++;
		} while(k <= maxItn);
		System.out.print("IPF Iteration: " + k1 + '\t');
		System.out.println("Convergence: " + conv);
		System.out.println("Reach convergence at....");
		System.out.print("IPF Iteration: " + k1 + '\t');
		System.out.println("Convergence: " + conv);
		
		return cij;	
	}
	
}