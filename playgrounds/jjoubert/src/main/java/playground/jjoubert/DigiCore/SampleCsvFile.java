/* *********************************************************************** *
 * project: org.matsim.*
 * SampleCsvFile.java
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

package playground.jjoubert.DigiCore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.DateString;
import playground.jjoubert.Utilities.MyStringBuilder;

public class SampleCsvFile {

	private static final java.util.logging.Logger log = Logger.getLogger(SampleCsvFile.class);

	/*=============================================================================
	 * String value indicating where the root where job is executed. 				|
	 * 		- Mac																	|
	 * 		- IVT-Sim0																|
	 * 		- Satawal																|
	 * 		- IE-Calvin														  		|
	 *=============================================================================*/
//	private static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; 	// Mac
//	private static String root = "/home/jjoubert/";										// IVT-Sim0
//	private static String root = "/home/jjoubert/data/";								// Satawal
	private static String root = "/home/jwjoubert/MATSim/MATSimData/";					// IE-Calvin

	/*=============================================================================
	 * The year for which the DigiCore analysis is being done. Available years are:	|
	 * 		- 2008																	|														|
	 *=============================================================================*/
	private static int year = 2008;

	/**
	 * This class reads `n' lines from the input DigiCore file. 
	 * @param args
	 */
	public static void main(String[] args) {
		int n = Integer.parseInt(args[0]);
		
		MyStringBuilder sb = new MyStringBuilder(root, year);
		log.info("Reading " + n + " lines from " + sb.getDigiCoreCsvFilename());
		
		try {
			File f = new File(sb.getDigiCoreCsvFilename());
			Scanner s = new Scanner(new BufferedReader(new FileReader(f)));
			
			DateString ds = new DateString();
			File o = new File(f.getParent() + "Sample_" + String.valueOf(n) + "_" + ds.toString() + ".csv");
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(o));

			try{
				for(int i = 0; i < n; i++){
					if(s.hasNextLine()){
						String line = s.nextLine();
						bw.write(line);
						bw.newLine();
					} else{
						log.warning("Input file does not have sufficient lines. Only " + String.valueOf(i+1) + " read.");
					}
				}
			} finally{
				bw.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
