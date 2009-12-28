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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InputSurveyData {
	
	public void inputData(int nx,int[]ncx,int nRow,int nCol,String spt,int[] xCol,int yCol,double sValue,File inFile){
		GlobalVars.initialRij = new double[nRow][nCol];	
		try{
			String s1, value; 
			int col = 0;
			int row = 0;			
			int pos = 0;
			int[] x = new int[nx];
			int y = 0;
			int sumncx=1;
			BufferedReader in = new BufferedReader(new FileReader(inFile));
						
			s1= in.readLine();		//read out the data heading	
			while ((s1 = in.readLine()) != null){
				col = 0;
				row = row +1;
				int found = -2 ;
				while (found != -1){
					found = s1.indexOf(spt);
					if (found != -1) {
						value = s1.substring(0,found);
						s1 = s1.substring(found+1);						
					}
					else{
						value = s1;
					}
					col = col+1;
					for(int c=0;c<nx;c++){
						if (col==xCol[c]) x[c]=Integer.parseInt(value)-1;
					}	
					if(col==yCol) y=Integer.parseInt(value)-1;		
				}
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
				GlobalVars.initialRij[pos][y] =GlobalVars.initialRij[pos][y]+1;					
			}
			for(int i=0;i<nRow;i++){
				for(int j=0;j<nCol;j++){
					if(GlobalVars.initialRij[i][j]==0) GlobalVars.initialRij[i][j]=sValue;
				}
			}
			in.close();			
		} catch(EOFException e){
			System.out.println("End of stream");
		} catch (IOException e){
			System.out.println(e.getMessage());
		}
	}	

}
