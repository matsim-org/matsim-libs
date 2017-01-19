/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import com.vividsolutions.jts.geom.Coordinate;

/**
* @author ikaddoura
*/

public class TrafficItemXMLReader extends MatsimXmlParser {

	private List<TrafficItem> trafficItems = new ArrayList<>();
	
	private boolean inLevelTrafficMl_Realtime = false;
	private boolean inLevelRWS = false;
	private boolean inLevelRW = false;
	private boolean inLevelFIS = false;
	private boolean inLevelFI = false;
	private boolean inLevelSHP = false;
	private boolean inLevelCF = false;
	
	private String timeStamp = null;
	private int fc = Integer.MIN_VALUE;
	private double length = Double.MIN_VALUE;
	private double confidence = Double.MIN_VALUE;
	private double freespeed = Double.MIN_VALUE;
	private double jamfactor = Double.MIN_VALUE;
	private double actualSpeed = Double.MIN_VALUE;

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		// set the level information
		if (name.equals("TRAFFICML_REALTIME")) {
			inLevelTrafficMl_Realtime = true;
			timeStamp = atts.getValue("CREATED_TIMESTAMP");
		} else if (name.equals("RWS")) {
			inLevelRWS = true;
		} else if (name.equals("RW")) {
			inLevelRW = true;
		} else if (name.equals("FIS")) {
			inLevelFIS = true;
		} else if (name.equals("FI")) {
			inLevelFI = true;
		} else if (name.equals("SHP")) {
			inLevelSHP = true;
		} else if (name.equals("CF")) {
			inLevelCF = true;
		}
		
		if (inLevelTrafficMl_Realtime
				&& inLevelRWS
				&& inLevelRW
				&& inLevelFIS
				&& inLevelFI) {
			
			if (inLevelSHP) {
				fc = Integer.valueOf(atts.getValue("FC"));
				length = Double.valueOf(atts.getValue("LE"));
			}
			
			if (inLevelCF) {
				confidence = Double.valueOf(atts.getValue("CN"));
				
				if (confidence > -1.) {
					freespeed = Double.valueOf(atts.getValue("FF"));
					jamfactor = Double.valueOf(atts.getValue("JF"));
					actualSpeed = Double.valueOf(atts.getValue("SU"));
				}
			}
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		if (inLevelTrafficMl_Realtime
				&& inLevelRWS
				&& inLevelRW
				&& inLevelFIS
				&& inLevelFI
				&& inLevelSHP) {
			
			String[] coordinatesString = content.split(" ");
			Coordinate[] coordinates = new Coordinate[coordinatesString.length];
			
			int counter = 0;
			for (String coordinate : coordinatesString) {
				String[] xy = coordinate.split(",");
				Coordinate coord = new Coordinate(Double.valueOf(xy[0]) , Double.valueOf(xy[1]));
				coordinates[counter] = coord;
				counter++;
			}
			
			if (confidence > -1) {
				TrafficItem item = new TrafficItem(String.valueOf(counter), timeStamp, fc, length, confidence, freespeed, jamfactor, actualSpeed, coordinates);
				trafficItems.add(item);
				counter++;
			}
		}
		
		// set the level information
		if (name.equals("TRAFFICML_REALTIME")) {
			inLevelTrafficMl_Realtime = false;
		} else if (name.equals("RWS")) {
			inLevelRWS = false;
		} else if (name.equals("RW")) {
			inLevelRW = false;
		} else if (name.equals("FIS")) {
			inLevelFIS = false;
		} else if (name.equals("FI")) {
			inLevelFI = false;
		} else if (name.equals("SHP")) {
			inLevelSHP = false;
		} else if (name.equals("CF")) {
			inLevelCF = false;
		}
	}

	public List<TrafficItem> getTrafficItems() {
		return trafficItems;
	}

	
}

