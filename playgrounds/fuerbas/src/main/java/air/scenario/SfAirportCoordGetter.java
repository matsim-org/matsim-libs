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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.apache.log4j.Logger;

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
	
	private static final String airportListFile = "";
	private static Map<String,Coord> airportListMap;
	
	private static void getAirportCoordFromWeb(String airportCode) throws IOException {
		String inputURL = "http://www.opennav.com/airport/"+airportCode;
		System.out.println(inputURL);
		
		URL oracle = new URL(inputURL);
		BufferedReader in = new BufferedReader(
		new InputStreamReader(oracle.openStream()));
		
		String inputLine;
		Coord airportCoord;
		
		while ((inputLine = in.readLine()) != null) { 
//			System.out.println(inputLine);
			if (inputLine.contains("LatLng = new GLatLng(")) { //read coordinate line
				//get coordinate information (LAT,LON) and transform to MATSim Coord (x=LON,y=LAT)
//				System.out.println("COORDS "+inputLine);
				String[] entries = inputLine.split(",");	//split into x and y part
//				System.out.println(entries[0]+"..."+entries[1]);
				String[] xLon = entries[1].split(";");
				String[] yLat = entries[0].split("GLatLng");
//				System.out.println("xLON: "+xLon[0]+" yLat :"+yLat[1]);
				String xCoord = xLon[0].replaceAll("\\)", "");	//remove brackets
				String yCoord = yLat[1].replaceAll("\\(", "");
				System.out.println("x: "+xCoord+" y: "+yCoord);
				airportCoord = new CoordImpl(xCoord, yCoord);	//create Coord
				airportListMap.put(airportCode, airportCoord);	//add airport code and Coord to airport list
				break;
			}
			
		}
		in.close();
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		getAirportCoordFromWeb("TXL");

	}

}
