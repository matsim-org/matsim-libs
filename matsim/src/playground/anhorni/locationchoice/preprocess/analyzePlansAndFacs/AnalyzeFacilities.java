/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSelectedPlansTables.java
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

package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

public class AnalyzeFacilities {

	private Facilities facilities;
	private final static Logger log = Logger.getLogger(AnalyzeFacilities.class);
	
	public void run(Facilities facilities, NetworkLayer network) {
		this.facilities = facilities;

		write("./output/facilities_activities_summary.txt", network);
		log.info("finished");
	}

	private void write(String outfile, NetworkLayer network) {

		
		String[] types = {"shop", "leisure"};		
		String[][] NOGA = {
				{
					"B015211A",
					"B015211B",	
					"B015211C",	
					"B015211D",	
					"B015211E",
					"B015212A",	
					"B015212B",	
					"B015221A",	
					"B015222A",	
					"B015223A",	
					"B015224A",	
					"B015225A",	
					"B015226A",	
					"B015227A",	
					"B015227B",	
					"B015231A",	
					"B015232A",	
					"B015233A",	
					"B015233B",	
					"B015241A",	
					"B015242A",	
					"B015242B",	
					"B015242C",	
					"B015242D",	
					"B015242E",	
					"B015243A",	
					"B015243B",	
					"B015244A",	
					"B015244B",	
					"B015244C",	
					"B015245A",	
					"B015245B",	
					"B015245C",	
					"B015245D",	
					"B015245E",	
					"B015246A",	
					"B015246B",	
					"B015247A",	
					"B015247B",	
					"B015247C",	
					"B015248A",	
					"B015248B",	
					"B015248C",	
					"B015248D",	
					"B015248E",	
					"B015248F",	
					"B015248G",	
					"B015248H",	
					"B015248I",	
					"B015248J",	
					"B015248K",	
					"B015248L",	
					"B015248M",	
					"B015248N",	
					"B015248O",	
					"B015248P",	
					"B015250A",	
					"B015250B",	
					"B015261A",	
					"B015262A",	
					"B015263A",	
					"B015271A",	
					"B015272A",	
					"B015273A",	
					"B015274A"
							},
				{
					"B015511A",	
					"B015512A",	
					"B015521A",	
					"B015522A",	
					"B015523A",	
					"B015523B",	
					"B015523C",	
					"B015530A",	
					"B015540A",	
					"B015551A",	
					"B015552A",	
					"B019211A",	
					"B019212A",	
					"B019213A",	
					"B019220A",	
					"B019220B",	
					"B019231A",	
					"B019231B",	
					"B019231C",	
					"B019231D",	
					"B019232A",	
					"B019232B",	
					"B019233A",	
					"B019234A",	
					"B019234B",	
					"B019234C",	
					"B019234D",	
					"B019240A",	
					"B019240B",	
					"B019251A",	
					"B019252A",	
					"B019253A",	
					"B019261A",	
					"B019262A",	
					"B019262B",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null",
					"null"
				}
		};
		
		double[][] capacityCount = new double[2][65];
		int[][] count = new int[2][65];
		
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j <65; j++) {
				capacityCount[i][j] = 0.0;
				count[i][j] = 0;
			}
		}
			
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			final BufferedWriter outSummary = IOUtils.getBufferedWriter("output/facilities_summary.txt");
			
			outSummary.write("Total number of facilities: " + this.facilities.getFacilities().size());
			out.write("Type\t#activity facilities\tcapacity\tavg. capacity\n");
					
			for (int typeIndex = 0; typeIndex < 2; typeIndex++) {
				
				int numberOfActivityFacilities = 0;
				double totalCapacity = 0.0;
				int totalCount = 0;
				
				Iterator< ? extends Facility> facility_it = this.facilities.getFacilities().values().iterator();
				while (facility_it.hasNext()) {
					Facility facility = facility_it.next();
					
					numberOfActivityFacilities += facility.getActivityOptions().size();
					
					for (int i = 0; i < capacityCount[typeIndex].length; i++) {						
						if (facility.getActivityOption(NOGA[typeIndex][i]) != null) {
							Iterator<ActivityOption> options_it = facility.getActivityOptions().values().iterator();
							while (options_it.hasNext()) {
								ActivityOption actOpt = options_it.next();
								if (actOpt.getType().startsWith(types[typeIndex])) {
									capacityCount[typeIndex][i] += actOpt.getCapacity();
									count[typeIndex][i] += 1;
									totalCapacity += actOpt.getCapacity();
									totalCount++;
								}
							}
						}
					}
				}
				for (int i = 0; i < capacityCount[typeIndex].length; i++) {
					if (NOGA[typeIndex][i].equals("null")) continue;
					out.write(types[typeIndex] + " " + NOGA[typeIndex][i] + ": " + 
							count[typeIndex][i] + "\t" + capacityCount[typeIndex][i] + 
							"\t" + capacityCount[typeIndex][i] / count[typeIndex][i] + "\n");
				}
				out.write(types[typeIndex] + " " + totalCount + "\t" +  totalCapacity + "\t" + totalCapacity / totalCount + "\n");
				out.flush();
				if (typeIndex == 0) {
					outSummary.write("Total number of activity facilities: " + numberOfActivityFacilities);
				}
				outSummary.flush();
			}
			out.close();
			outSummary.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
