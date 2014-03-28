/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.gfipAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to initially scan gantry data.
 * 
 * @author jwjoubert
 */
public class ScanGantryData {
	private final static Logger LOG = Logger.getLogger(ScanGantryData.class);
	private static List<String> vln = new ArrayList<String>();
	private static List<String> gantries = new ArrayList<String>();
	private static List<String> days = new ArrayList<String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ScanGantryData.class.toString(), args);
		
		String input = args[0];
		parse(input);
		
		Header.printFooter();
	}
	
	public static void parse(String inputfile){
		LOG.info("Parsing " + inputfile);
		Counter counter = new Counter("   lines # ");

		BufferedReader br = IOUtils.getBufferedReader(inputfile);
		try{
			String line = br.readLine(); /* Header */
			String s_vln = null;
			String s_day = null;
			String s_gantry = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				s_vln = sa[0];
				if(!vln.contains(s_vln)){
					vln.add(s_vln);
				}
				
				s_day = sa[1].substring(0, 10);
				if(!days.contains(s_day)){
					days.add(s_day);
				}
				
				s_gantry = sa[2];
				if(!gantries.contains(s_gantry)){
					gantries.add(s_gantry);
				}
				counter.incCounter();
			}
			counter.printCounter();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + inputfile);
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + inputfile);
			}
		}
		LOG.info("Done parsing " + inputfile);
		
		LOG.info("---  Statistics  ---------------------------");
		LOG.info("  Number of unique IDs: " + vln.size());
		LOG.info("  Number of unique days: " + days.size());
		for(String s : days){
			LOG.info("       " + s);
		}
		LOG.info("  Number of unique gantries: " + gantries.size());
		for(String s : gantries){
			LOG.info("       " + s);
		}
	}

}
