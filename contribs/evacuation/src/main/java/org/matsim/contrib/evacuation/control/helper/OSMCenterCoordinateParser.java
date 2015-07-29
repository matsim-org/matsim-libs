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
package org.matsim.contrib.evacuation.control.helper;

import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class OSMCenterCoordinateParser extends MatsimXmlParser {
	
	double minLat = Double.POSITIVE_INFINITY;
	double maxLat = Double.NEGATIVE_INFINITY;
	double minLon = Double.POSITIVE_INFINITY;
	double maxLon = Double.NEGATIVE_INFINITY;

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if ("node".equals(name)) {
			double lat = Double.parseDouble(atts.getValue("lat"));
			if (lat > maxLat) {
				maxLat = lat;
			} 
			if (lat < minLat) {
				minLat = lat;
			}
			
			double lon = Double.parseDouble(atts.getValue("lon"));
			if (lon > maxLon) {
				maxLon = lon;
			}
			if (lon < minLon) {
				minLon = lon;
			}
			
		}

	}
	
	public double getCenterLat() {
		return (minLat+maxLat)/2;
	}
	public double getCenterLon() {
		return (minLon+maxLon)/2;
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub

	}

}
