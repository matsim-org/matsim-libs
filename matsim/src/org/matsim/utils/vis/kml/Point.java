/* *********************************************************************** *
 * project: org.matsim.*
 * Point.java
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

package org.matsim.utils.vis.kml;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://earth.google.com/kml/kml_tags_21.html#point">
 * http://earth.google.com/kml/kml_tags_21.html#point</a>
 */
public class Point extends Geometry {

	private double longitude;
	private double latitude;
	private double altitude;

	/**
	 * Constructs a new <code>Point</code> with all required attributes.
	 * 
	 * @param longitude
 	 * the longitude of the new 
 	 * <a href="http://earth.google.com/kml/kml_tags_21.html#point">
	 * Point</a> object.
	 * @param latitude
 	 * the latitude of the new 
 	 * <a href="http://earth.google.com/kml/kml_tags_21.html#point">
	 * Point</a> object.
	 * @param altitude
 	 * the altitude of the new 
 	 * <a href="http://earth.google.com/kml/kml_tags_21.html#point">
	 * Point</a> object.
	 */
	public Point(double longitude, double latitude, double altitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
		this.altitude = altitude;
	}

	/**
	 * 
	 * @return 
	 * the altitude of the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Point</a> object.
	 */
	public double getAltitude() {
		return altitude;
	}

	/**
	 * 
	 * @return 
	 * the latitude of the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Point</a> object.
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * 
	 * @return 
	 * the longitude of the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Point</a> object.
	 */
	public double getLongitude() {
		return longitude;
	}
	
	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Point>");
		out.newLine();

		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<coordinates>" + this.longitude + "," + this.latitude + "," + this.altitude + "</coordinates>");
		out.newLine();
		
		out.write(Object.getOffset(offset, offsetString));
		out.write("</Point>");
		out.newLine();
			
	}

	
}
