/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertNaToZero.java
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

package playground.jjoubert.TemporaryCode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class ConvertNaToZero {
	private final static Logger log = Logger.getLogger(ConvertNaToZero.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/Activities/GautengMinor_DistanceAdjacency";
		String inFilename = filename + ".txt";
		String outFilename = filename + "_0.txt";
		String delimiter = ",";
		log.info(" Converting 'NA' to zeros in adjacency file " + inFilename);
		int lineCounter = 0;
		int lineMultiplier = 1;
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(inFilename))));
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outFilename)));
			try{
				while(input.hasNextLine()){
					String[] line = input.nextLine().split(delimiter);
					for (int i = 0; i < line.length-1; i++){
						String s  = line[i];
						String ss = s.equalsIgnoreCase("NA") ? "00" : s;
						output.write(ss);
						output.write(delimiter);
					}
					// Last value without delimiter
					int i = line.length-1;
					String s  = line[i];
					String ss = s.equalsIgnoreCase("NA") ? "00" : s;
					output.write(ss);
					
					output.newLine();
					
					lineCounter++;
					// Report progress
					if(lineCounter == lineMultiplier){
						log.info("   Lines converted: " + String.valueOf(lineCounter));
						lineMultiplier *= 2;
					}
				}
				log.info("   Lines converted: " + String.valueOf(lineCounter) + " (Done)");
			} finally{
				output.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
