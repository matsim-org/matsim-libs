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
package org.matsim.contrib.matsim4urbansim.utils.io.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author thomas
 *
 */
public class SetPlotRange {
	
	private static final String TAB = "\t";
	private static final String NA  = "NA";
	
	public static void main(String args[]){
		
		String sourceFile = args[0];
		String destinationFile = args[1];
		double maxValue = Double.parseDouble(args[2]);
		double minValue = Double.parseDouble(args[3]);
		
		BufferedReader br = IOUtils.getBufferedReader(sourceFile);
		BufferedWriter bw = IOUtils.getBufferedWriter(destinationFile);
		
		try{
			String line;
			line = br.readLine();
			bw.write(line);
			bw.newLine();
			
			String [] parts;
			
			while( (line = br.readLine()) != null ){
				
				parts = line.split(TAB);
				
				for(int i = 0; i < parts.length; i++){
					
					if(i == 0) // y coordinate
						bw.write(parts[i]);
					else{
						if(parts[i].equalsIgnoreCase(NA))
							bw.write(parts[i]);
						else{
							double value = Double.parseDouble(parts[i]);
							if(value < minValue)
								bw.write(String.valueOf(minValue));
							else if(value > maxValue)
								bw.write(String.valueOf(maxValue));
							else
								bw.write(parts[i]);
						}
					}
					bw.write(TAB);
				}
				bw.newLine();
			}
			bw.flush();
			bw.close();
			br.close();
			
			System.out.println("Done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
