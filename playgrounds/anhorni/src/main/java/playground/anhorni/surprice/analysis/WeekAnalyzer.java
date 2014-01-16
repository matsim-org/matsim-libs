/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import playground.anhorni.surprice.Surprice;

public class WeekAnalyzer {
	private final static Logger log = Logger.getLogger(WeekAnalyzer.class);	 		
	private Bins tolltdBins = new Bins(1, Surprice.numberOfIncomeCategories, "WeeklyTolltdPerIncome");
		
	public static void main (final String[] args) {		
		if (args.length != 2) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		String inpath = args[0];
		String outpath = args[1];
		WeekAnalyzer analyzer = new WeekAnalyzer();
		analyzer.run(inpath, outpath);	
	}
		
	public void run(String inpath, String outpath) {
		log.info("Starting analysis ============================================================= ");
		for (String day : Surprice.days) {
			log.info("Analyzing " + day + " --------------------------------------------");
			String analysisFile = inpath + "/" + day + "/" + day + ".tolltdPerIncome.txt";
			try {
		          final BufferedReader in = new BufferedReader(new FileReader(analysisFile));
		          String curr_line = in.readLine(); // Skip header
		          while ((curr_line = in.readLine()) != null) {	
			          String parts[] = curr_line.split("\t");
			          int incomeGroup = Integer.parseInt(parts[0]);  
			          double tdToll = Double.parseDouble(parts[1]);
			          
			          this.tolltdBins.addVal(incomeGroup, tdToll);
		          }
		          in.close();
		          
		        } // end try
		        catch (IOException e) {
		        	e.printStackTrace();
		        }		
		}	
		this.tolltdBins.plotBinnedDistribution(outpath + "/", "income", "");
		log.info("=================== Finished analyses ====================");
	}
}
