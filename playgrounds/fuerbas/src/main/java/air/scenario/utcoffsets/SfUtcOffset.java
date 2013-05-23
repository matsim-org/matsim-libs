/* *********************************************************************** *
 * project: org.matsim.*
 * SfUtcOffset
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package air.scenario.utcoffsets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Reads UTC Offsets for all airports in a OSM file from the webservice www.earthtools.org.
 * 
 * The response from the webservice is a xml document, example:
 *<?xml version="1.0" encoding="ISO-8859-1" ?>
 *<timezone xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://www.earthtools.org/timezone-1.1.xsd">
 *	<version>1.1</version>
 *	<location>
 *		<latitude>41.8543</latitude>
 *  	<longitude>12.6453</longitude>
 * 	</location>
 * 	<offset>1</offset>
 * 	<suffix>A</suffix>
 * 	<localtime>25 Jan 2012 15:30:59</localtime>
 *	<isotime>2012-01-25 15:30:59 +0100</isotime>
 *	<utctime>2012-01-25 14:30:59</utctime>
 * 	<dst>False</dst>
 *</timezone>
 * 
 * 
 * @see http://www.earthtools.org/webservices.htm
 * 
 * @author sfuerbas
 * @author dgrether
 *
 */

public class SfUtcOffset {
	
	private static final Logger log = Logger.getLogger(SfUtcOffset.class);
	
//	private static final String INPUTFILE_OSM = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
//	private static final String OUTPUTFILE_UTC= "/media/data/work/repos/"
//				+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
	
	private static final String INPUTFILE_AIRPORTS = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\worldwide_airports_with_coords.csv";
	private static final String OUTPUTFILE_UTC= "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\utc_offsets.txt";
	
	private Map<String, Coord> airports = new HashMap<String,Coord>();
	
	
	public void writeUtcOffset(String inputAirportListFile, String outputFile) throws IOException, InterruptedException{

		BufferedReader br = new BufferedReader(new FileReader(new File(inputAirportListFile)));
		while (br.ready()) {
			String line = br.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0];
			String xCoord = entries[1];
			String yCoord = entries[2];
			this.airports.put(airportCode, new CoordImpl(xCoord,yCoord));
		}
		
		br.close();
		
		log.warn("This tool creates UTC offsets, however ignores daylight saving time");
		log.warn("Using the following inputfile: "+inputAirportListFile);
		
		log.info("Getting UTC offsets for " + this.airports.size() + " airports. This will take 2 seconds per airport, at least...");
		int count = 0;
		Map<String, Double> airportUTCOffsetMap = new HashMap<String, Double>();
		for (Entry<String, Coord> e : this.airports.entrySet()) {
			Double utcOffset = getUtcOffset(this.airports.get(e.getKey()));
			log.info("  Got offset for airport: " + e.getKey() + " offset: " + utcOffset);
			airportUTCOffsetMap.put(e.getKey(), utcOffset);
			count++;
			if (count % 100 == 0){
				log.info("  Got UTC offset for " + count + " airports of in total " + this.airports.size() + " airports");
			}
		}
		
		log.info("Writing to file: " + outputFile);
		//Output
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
		for (Entry<String, Double> e : airportUTCOffsetMap.entrySet()) {
			bw.write(e.getKey().toString() + "\t" +  e.getValue());
			bw.newLine();
		}
		bw.close();
	}
	
	public static Double getUtcOffset(Coord coord) throws IOException, InterruptedException {
		
		Thread.sleep(4*1000); //requirement by the web service provider
		double xCoord = Math.round(coord.getX()*1000.)/1000.;
		double yCoord = Math.round(coord.getY()*1000.)/1000.;
		String inputURL = "http://www.earthtools.org/timezone/"+yCoord+"/"+xCoord;	// MATSim y-axis used for latitude (north-south), x-axis for longitude (east-west)
		
		System.out.println(inputURL);
		
		URL oracle = new URL(inputURL);
		BufferedReader in = new BufferedReader(
		new InputStreamReader(oracle.openStream()));
		
		String inputLine;
		double offset = 0;
		
		while ((inputLine = in.readLine()) != null) {
			log.debug(inputLine);
			if (inputLine.contains("offset")) {
				inputLine = inputLine.replaceAll("<offset>", "");
				inputLine = inputLine.replaceAll("</offset>", "");
				inputLine = inputLine.replaceAll(" ", "");
				offset = Double.parseDouble(inputLine);
//				System.out.println(offset);
			}
// currently ignore daylight saving time
			//			if (inputLine.contains("dst")) {
//				inputLine = inputLine.replaceAll("<dst>", "");
//				inputLine = inputLine.replaceAll("</dst>", "");
//				inputLine = inputLine.replaceAll(" ", "");
//				if (inputLine.equalsIgnoreCase("true"))  {
//					offset++;
//				}
//				System.out.println("neu"+offset);
//				System.out.println(inputLine);
//			}
		}
		in.close();
		return offset;
	}

	public static void main(String args[]) throws Exception {
		SfUtcOffset test = new SfUtcOffset();
		test.writeUtcOffset(INPUTFILE_AIRPORTS, OUTPUTFILE_UTC);
	}

	
}
