/* *********************************************************************** *
 * project: org.matsim.*
 * X2GoogleMap.java
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
package playground.yu.utils.googleMap;

import java.text.DecimalFormat;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author Chen
 * 
 */
public abstract class X2GoogleMap {
	/** converts coordinate system to WGS84 */
	protected CoordinateTransformation coordTransform;

	protected static String URL_HEADER = "http://maps.google.com/maps/api/staticmap?",
			MARKERS = "&markers=",
			COLOR = "color:",
			LABEL = "label:",
			SEPERATOR_IN_PARAMETER = "|",
			PATH = "&path=",
			SENSOR = "&sensor=",
			DEFAULT_LABEL_COLOR = "blue",
			COORDINATE_SEPERATOR = ",",
			DEFAULT_PATH_COLOR = "0x0000ff",
			WEIGHT = "weight:",
			SIZE = "&size=",
			DEFAULT_SIZE = "1024x768",
			DEFAULT_SENSOR = "false";
	protected static int DEFAULT_WEIGHT = 5, COLOR_MAX = 256;
	public static String DEFAULT_URL_PREFIX = URL_HEADER + SIZE + DEFAULT_SIZE,
			DEFAULT_URL_POSTFIX = SENSOR + DEFAULT_SENSOR;
	protected static DecimalFormat formatter = new DecimalFormat("###.######");

	public X2GoogleMap(String fromSystem) {
		coordTransform = TransformationFactory.getCoordinateTransformation(
				fromSystem, TransformationFactory.WGS84);
	}

	/**
	 * @param coord
	 *            original coordinate in MATSim files
	 * @return
	 */
	protected String createCoordinate(Coord coord) {
		coord = coordTransform.transform(coord);
		StringBuffer strBuf = new StringBuffer(formatter.format(coord.getY()));
		strBuf.append(COORDINATE_SEPERATOR);
		strBuf.append(formatter.format(coord.getX()));

		return strBuf.toString();
	}

	protected String createMarker(Coord coord, String label, String color) {
		StringBuffer strBuf = new StringBuffer(MARKERS);
		strBuf.append(COLOR);
		strBuf.append(color);
		strBuf.append(SEPERATOR_IN_PARAMETER);
		strBuf.append(LABEL);
		strBuf.append(label);
		strBuf.append(SEPERATOR_IN_PARAMETER);
		strBuf.append(this.createCoordinate(coord));

		return strBuf.toString();
	}

	/**
	 * @param linkIds
	 * @param color
	 *            color of path incl. transparency
	 * @return
	 */
	protected String createPath(List<Coord> coords, String color, int weight) {
		StringBuffer strBuf = new StringBuffer(PATH);
		strBuf.append(COLOR);
		strBuf.append(color);
		strBuf.append(SEPERATOR_IN_PARAMETER);
		strBuf.append(WEIGHT);
		strBuf.append(weight);
		strBuf.append(SEPERATOR_IN_PARAMETER);

		strBuf.append(this.createCoordinate(coords.remove(0)/* first point */));
		for (Coord coord : coords) {
			strBuf.append(SEPERATOR_IN_PARAMETER);
			strBuf.append(this.createCoordinate(coord));
		}
		return strBuf.toString();
	}

	public abstract String getGoogleMapURL();
}
