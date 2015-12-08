/* *********************************************************************** *
 * project: org.matsim.*
 * FilterCordonLinksFromLinkStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package herbie.running.analysis;

import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FilterCordonLinksFromLinkStats {

	private static ArrayList<String> cordonLinkIds = new ArrayList<String>();
	private static ArrayList<String> cordonLinkStats = new ArrayList<String>();
	

	public static void main(String[] args) throws IOException {
		
		
		System.out.println("Read cordon link ids...");
		String cordonLinkFile = "P:\\Projekte\\herbie\\CaseStudies\\MAUT\\CordonLinks.txt";
		readCordonLinks(cordonLinkFile);
		System.out.println("... done.");
		System.out.println("# cordon links = "+cordonLinkIds.size());
		System.out.println();
		
		System.out.println("Read linkstats and save lines containing the cordon links ...");
		String linkStatsFile = "P:\\Projekte\\herbie\\output\\Report\\Base scenario\\run0\\ITERS\\it.150\\herbie.150.linkstats.txt.gz";
		readCordonLinkStats(linkStatsFile);		
		System.out.println("... done.");
		System.out.println("# cordon links = "+cordonLinkStats.size());
		System.out.println();
		
		System.out.println("Write linkstats for  cordon links ...");
		String cordonLinkStatsFile = "P:\\Projekte\\herbie\\Bericht\\Excel_Dateien\\cordonLinkStats_noPricing.txt";
		writeCordonLinkStats(cordonLinkStatsFile);	
		System.out.println("... done.");
		System.out.println();
		

	}
	
	
	public static void readCordonLinks(String cordonLinkFile) throws IOException {
		
		BufferedReader in = IOUtils.getBufferedReader(cordonLinkFile);
		in.readLine();
		String inputLine;
		while((inputLine = in.readLine()) != null) {
			String[] entries = inputLine.split("\t");
			cordonLinkIds.add(entries[0]);
		}	
	}
	
	
	public static void readCordonLinkStats(String cordonLinkFile) throws IOException {
		
		BufferedReader in = IOUtils.getBufferedReader(cordonLinkFile);
		cordonLinkStats.add(in.readLine());
		String inputLine;
		while((inputLine = in.readLine()) != null) {			
			String[] entries = inputLine.split("\t");
			if (cordonLinkIds.contains(entries[0])) {
				cordonLinkStats.add(inputLine);
			}
		}	
	}
	
	public static void writeCordonLinkStats(String cordonLinkStatsFile) throws IOException {
		
		BufferedWriter out = IOUtils.getBufferedWriter(cordonLinkStatsFile);
		for (String line : cordonLinkStats) {
			out.write(line);
			out.newLine();
		}
		out.close();		
	}
}
