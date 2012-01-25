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
package air.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author sfuerbas
 * @author dgrether
 *
 */
public class SfUtcOffset {
	
	private static final Logger log = Logger.getLogger(SfUtcOffset.class);
	
	private static final String INPUTFILE_OSM = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
	private static final String OUTPUTFILE_UTC= "/media/data/work/repos/"
				+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
	
	private Map<String, Coord> airportsInOsm;
	
	
	public void writeUtcOffset(String inputOsmFile, String outputFile) throws IOException, InterruptedException{
		SfOsmAerowayParser osmReader = new SfOsmAerowayParser(
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84));
		osmReader.parse(inputOsmFile);
	
		this.airportsInOsm = osmReader.airports;
		log.info("Getting UTC offsets for " + this.airportsInOsm.size() + " airports. This will take 2 seconds per airport, at least...");
		int count = 0;
		Map<String, Double> airportUTCOffsetMap = new HashMap<String, Double>();
		for (Entry<String, Coord> e : this.airportsInOsm.entrySet()) {
			Double utcOffset = getUtcOffset(this.airportsInOsm.get(e.getKey()));
			log.info("  Got offset for airport: " + e.getKey() + " offset: " + utcOffset);
			airportUTCOffsetMap.put(e.getKey(), utcOffset);
			count++;
			if (count % 100 == 0){
				log.info("  Got UTC offset for " + count + " airports of in total " + this.airportsInOsm.size() + " airports");
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
		
		Thread.sleep(2*1000); //requirement by the web service provider
		String inputURL = "http://www.earthtools.org/timezone/"+coord.getX()+"/"+coord.getY();	
		
		URL oracle = new URL(inputURL);
		BufferedReader in = new BufferedReader(
		new InputStreamReader(oracle.openStream()));
		
		String inputLine;
		double offset = 0;
		
		while ((inputLine = in.readLine()) != null) {
			
			if (inputLine.contains("offset")) {
				inputLine = inputLine.replaceAll("<offset>", "");
				inputLine = inputLine.replaceAll("</offset>", "");
				inputLine = inputLine.replaceAll(" ", "");
				offset = Double.parseDouble(inputLine);
//				System.out.println(offset);
			}
			else if (inputLine.contains("dst")) {
				inputLine = inputLine.replaceAll("<dst>", "");
				inputLine = inputLine.replaceAll("</dst>", "");
				inputLine = inputLine.replaceAll(" ", "");
				if (inputLine.equalsIgnoreCase("true"))  {
					offset++;
				}
//				System.out.println("neu"+offset);
//				System.out.println(inputLine);
			}
		}
		in.close();
		return offset;
	}

	public static void main(String args[]) throws Exception {
		SfUtcOffset test = new SfUtcOffset();
		test.writeUtcOffset(INPUTFILE_OSM, OUTPUTFILE_UTC);
	}

	
}
