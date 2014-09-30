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

package playground.juliakern.toi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ReadCsv {

	/**
	 * @param args
	 */

	static String csvFile; 
	String cvsSplitBy = ",";
	
		  public static void main(String[] args) {
			  
				ReadCsv csv = new ReadCsv();
				if(args.length>0){
					csvFile=args[0];
					csv.run(csvFile);
				}
			 
			  }
			 
			  public void run(String csvFile) {
			 
				BufferedReader br = null;
				String line = "";
			 
				try {
			 
					br = new BufferedReader(new FileReader(csvFile));
					br.readLine(); //skip first line
					while ((line = br.readLine()) != null) {
			 
					        // use comma as separator
						String[] trip = line.split(cvsSplitBy);
			 
						
						//System.out.println("Country [code= " + country[4]          + " , name=" + country[5] + "]");
			 
					}
			 
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			 
				System.out.println("Done");
			  }
		
	}


