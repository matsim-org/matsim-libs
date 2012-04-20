/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package air.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author sfuerbas
 *
 */
public class SfAirportCoordGetter {

	/**
	 * This tool is designed to download airport coordinates from the opennav.com database. Input data needed are the IATA airport codes.
	 * 
	 * WORK IN PROGRESS, NOT FINISHED, YET FUNCTIONAL FOR SINGLE AIRPORT QUERIES
	 */
	
	private static final Logger log = Logger.getLogger(SfAirportCoordGetter.class);
	
	private static final String airportListInput = "Z:\\WinHome\\shared-svn\\projects\\throughFlightData\\oag_rohdaten\\OAGSEP09.CSV";
	private static final String airportListOutput = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\worldwide_airports_with_coords.csv";
	private static final String airportMissingOutput = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\worldwide_airports_missing.csv";
	private static Map<String,Coord> airportListMap = new HashMap<String, Coord>();
	private static Set<String> missingAirports = new HashSet<String>();
	private int count = 0;
	
	private static boolean getAirportCoordFromOpennav(String airportCode) throws IOException {
		
		boolean retrieved = false;
		String inputURL = "http://www.opennav.com/airport/"+airportCode;
		
		URL oracle = new URL(inputURL);
		BufferedReader in = new BufferedReader(
		new InputStreamReader(oracle.openStream()));
		
		String inputLine;
		Coord airportCoord;
		
		while ((inputLine = in.readLine()) != null) { 
			if (inputLine.contains("No such airport was found")) {
				log.error("No Coordinates for "+airportCode+" could be retrieved.");
				missingAirports.add(airportCode);
				break;
			}
			if (inputLine.contains("LatLng = new GLatLng(")) { //read coordinate line
				//get coordinate information (LAT,LON) and transform to MATSim Coord (x=LON,y=LAT)
				String[] entries = inputLine.split(",");	//split into x and y part
				String[] xLon = entries[1].split(";");
				String[] yLat = entries[0].split("GLatLng");
				String xCoord = xLon[0].replaceAll("\\)", "");	//remove brackets
				String yCoord = yLat[1].replaceAll("\\(", "");
				try {
					airportCoord = new CoordImpl(xCoord, yCoord);	//create Coord
					airportListMap.put(airportCode, airportCoord);
					retrieved=true;
				}
				catch(NumberFormatException e) {
					missingAirports.add(airportCode);
					log.error("Wrong Coordinate Format for airport: "+airportCode+" "+e.getMessage());
					break;
				}
				break;
			}
		}
		in.close();
		return retrieved;
	}
	
	private static void getAirportCoordFromGcmap(String airportCode) throws IOException {
		String inputURL = "http://www.gcmap.com/airport/"+airportCode;
		
		URL oracle = new URL(inputURL);
		BufferedReader in = new BufferedReader(
		new InputStreamReader(oracle.openStream()));
		
		String inputLine;
		Coord airportCoord;
		
		String[] entries;
		String[] xLon;
		String[] yLat;
		String xCoord="NaN";
		String yCoord="NaN";
		
		boolean xRet=false;	//check if coordinates have been retrieved
		boolean yRet=false;
		
		while ((inputLine = in.readLine()) != null) {
			
//			System.out.println(inputLine);

			if (inputLine.contains("latitude")) { //read coordinate line
				//get coordinate information (LAT,LON) and transform to MATSim Coord (x=LON,y=LAT)
				entries = inputLine.split("title");	
				yLat = entries[1].split("\"");
				yCoord = yLat[1].toString();
				yRet=true;
			}
			
			if (inputLine.contains("longitude")) { //read coordinate line
				//get coordinate information (LAT,LON) and transform to MATSim Coord (x=LON,y=LAT)
				entries = inputLine.split("title");	//split into x and y part
				xLon = entries[1].split("\"");
//				log.info("GMAP X COORD: "+xLon[1]);
				xCoord = xLon[1].toString();	
				xRet=true;
			}				
			
			if (xRet&&yRet) {
					try {
					airportCoord = new CoordImpl(xCoord, yCoord);	//create Coord
					airportListMap.put(airportCode, airportCoord);
					missingAirports.remove(airportCode);
				}
				catch(NumberFormatException e) {
					missingAirports.add(airportCode);
					log.error("Wrong Coordinate Format for airport: "+airportCode+" "+e.getMessage());
					break;
				}
			}
		}
		in.close();
	}
	
	private void writeToFile(Map<String,Coord> airportMap, String outputFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
		Iterator it = airportListMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			bw.write(pairs.getKey().toString() + "\t" + airportListMap.get(pairs.getKey()).getX()
					+ "\t" + airportListMap.get(pairs.getKey()).getY());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	
	private void fillAirportMap(String inputOagFile) throws IOException {
		BufferedReader brOag = new BufferedReader(new FileReader(new File(inputOagFile)));
		while (brOag.ready()) {
			String oneLine = brOag.readLine();
			String[] lineEntries = oneLine.split(",");
			String iataCode1 = lineEntries[4];
			iataCode1 = iataCode1.replaceAll("\"", "");
			String iataCode2 = lineEntries[7];
			iataCode2 = iataCode2.replaceAll("\"", "");
			if (!airportListMap.containsKey(iataCode1) && !missingAirports.contains(iataCode1)) {
				boolean opennav = getAirportCoordFromOpennav(iataCode1);
				if (!opennav) {
					log.info("Getting coordinates from GCMAP");
					getAirportCoordFromGcmap(iataCode1);
				}
				log.info("Coordinates for "+iataCode1+" are: "+airportListMap.get(iataCode1));
				count++;
			}
			if (!airportListMap.containsKey(iataCode2) && !missingAirports.contains(iataCode2)){
				boolean opennav = getAirportCoordFromOpennav(iataCode2);
				if (!opennav) {
					log.info("Getting coordinates from GCMAP");
					getAirportCoordFromGcmap(iataCode2);
				}
				log.info("Coordinates for "+iataCode2+" are: "+airportListMap.get(iataCode2));
				count++;
			}
		}
		if (count % 100 == 0){
			log.info("Got coordinates for a total of "+count+" airports.");
		}
		brOag.close();
	}
	
	private void writeMissingAirports(Set<String> missingAD, String outputFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
		for (String airportCode : missingAirports) {
			bw.write(airportCode);
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	
	public static void main(String[] args) throws IOException {
		
		SfAirportCoordGetter getter = new SfAirportCoordGetter();
		getter.fillAirportMap(airportListInput);
		getter.writeToFile(airportListMap, airportListOutput);
		getter.writeMissingAirports(missingAirports, airportMissingOutput);
		log.info("Coordinates for "+getter.count+" airports have been found, coordinates for "+missingAirports.size()+" could not be found. " +
				"Output has been written to: "+airportListOutput);
		
	}

}
