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

public class InputPdf {
		
	public void inputDistribution(int z, int nCol, String spt,File inFile){
			GlobalVars.fixedC = new double[nCol];
			try{
				String s1, value;
				int col = 0;					
					
				BufferedReader in = new BufferedReader(new FileReader(inFile));			
				s1= in.readLine();		//read out the heading	
				for (int n = 1; n < z; n++){
					s1= in.readLine();
				}	
					s1 = in.readLine();
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
						if (col >= 3 && col <= nCol+2) GlobalVars.fixedC [col-3] = Double.parseDouble(value);					
					}		
				in.close();	
			} catch(EOFException e){
				System.out.println("End of stream");
			} catch (IOException e){
				System.out.println(e.getMessage());
			}
		}
		
	}


