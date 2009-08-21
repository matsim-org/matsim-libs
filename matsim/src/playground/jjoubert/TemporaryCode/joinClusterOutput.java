/* *********************************************************************** *
 * project: org.matsim.*
 * joinClusterOutput.java
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

public class joinClusterOutput {
	private final static Logger log = Logger.getLogger(joinClusterOutput.class);

	public static void main(String [] args){
		String input01 = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/Activities/GautengMinorClusterOutput_20_30.txt";
		String input02 = "/Users/johanwjoubert/R-Source/Code/bt.txt";
		String input03 = "/Users/johanwjoubert/R-Source/Code/dc.txt";
		String input04 = "/Users/johanwjoubert/R-Source/Code/ec.txt";
		
		String outputFilename = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/Activities/GautengMinorClusterSNA_20_30.txt";
		
		
		try {
			Scanner inputCluster = new Scanner(new BufferedReader(new FileReader(new File(input01))));
			Scanner inputBetween = new Scanner(new BufferedReader(new FileReader(new File(input02))));
			Scanner inputDegree = new Scanner(new BufferedReader(new FileReader(new File(input03))));
			Scanner inputEigen = new Scanner(new BufferedReader(new FileReader(new File(input04))));

			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFilename)));

			int lineCounter = 0;
			try{
				output.write("ClusterId,Long,Lat,Activities,Between,Degree,Eigen");
				output.newLine();
				inputCluster.nextLine();
				while(inputCluster.hasNextLine()){
					String lineString = inputCluster.nextLine();
					String []line = lineString.split(",");
					if(line.length == 4){
						output.write(lineString);
						output.write(",");
						output.write(inputBetween.nextLine());
						output.write(",");
						output.write(inputDegree.nextLine());
						output.write(",");
						output.write(inputEigen.nextLine());
						output.newLine();						
						lineCounter++;
					}
				}
			} finally{
				output.close();
			}
			log.info("Number of lines processed: " + lineCounter);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
