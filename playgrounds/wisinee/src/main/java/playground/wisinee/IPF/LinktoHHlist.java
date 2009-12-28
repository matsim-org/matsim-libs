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

import java.util.Random;

public class LinktoHHlist {
	public void link(int np, int nx, int[] ncx, int nRow, int nCol, String spt){		
		GlobalVars.countUsedPP = new double[nRow][nCol];
		GlobalVars.totalPP = new double[nCol+1];
		GlobalVars.usedPP = new double[nCol+1];
		GlobalVars.finalData = new String[np];
		
		int x[] = new int[nx];
		
		
		int pos=0;
		int selectI = 0;
		int sumncx=1;
		String data ="";
		
		for (int p=0;p<np;p++){
			//get information on the independent variables
			data = GlobalVars.orgn.get(p).data;
			for (int i=0;i<nx;i++){
				x[i]  = GlobalVars.orgn.get(p).dataCol[i];							
			}					
			//calculation
			for (int i=0;i<nx;i++){
				if(i==0){
					pos = x[i];
				}
				else{
					for (int j=i;j>=1;j--){
						sumncx = sumncx*ncx[j-1];
					}
					pos = pos+x[i]*sumncx;
					sumncx=1;
				}									
			}
			for (int i=0;i<nCol;i++){
				GlobalVars.totalPP[i+1] = GlobalVars.finalRij[pos][i];
				GlobalVars.usedPP[i+1] = GlobalVars.countUsedPP[pos][i];
			}							
			selectI = randomPP(nCol); 
				
			GlobalVars.countUsedPP[pos][selectI-1] = GlobalVars.countUsedPP[pos][selectI-1] + 1;
				//prepare final data for write to file	
			GlobalVars.finalData[p] = data+spt+selectI;
		}		
	}
	
	
	private int randomPP(int nCol){
		double sumPP = 0, randomNum = 0;				
		int selectPP = 0;
		double[] cdfPP = new double[nCol+1];
		double[] pp = new double[nCol+1];		
		Random rand = new Random();
		
		for (int i = 1;i<=nCol;i++){
			pp[i] = GlobalVars.totalPP[i] - GlobalVars.usedPP[i];
			if (pp[i] < 1) pp[i] = 0;
				sumPP = sumPP + pp[i];							
		}
		
		if (sumPP<1) {
			sumPP = 0;
			for (int i = 1;i<=nCol;i++){
				pp[i] = GlobalVars.totalPP[i] - GlobalVars.usedPP[i];
				if (pp[i]<0) pp[i]=0;
				sumPP = sumPP + pp[i];							
			}
		}
		
		cdfPP[0] = 0;
		for (int i = 1; i<=nCol; i++){
			cdfPP[i] = cdfPP[i-1] + pp[i] / sumPP;				
		}	
		randomNum = rand.nextDouble();
		for (int i=1;i<=nCol;i++){
			if (randomNum > cdfPP[i-1] && randomNum <= cdfPP[i]){
				selectPP =i;
				break;
			}
		}
		return selectPP;
	}
	
}
