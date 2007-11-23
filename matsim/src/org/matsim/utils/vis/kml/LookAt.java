/* *********************************************************************** *
 * project: org.matsim.*
 * LookAt.java
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
 * <a href="http://earth.google.com/kml/kml_tags_21.html#lookat">
 * http://earth.google.com/kml/kml_tags_21.html#lookat</a>
 */
public class LookAt extends Object {

	private double longitude;
	private double latitude;
	private double range;
	private double tilt;
	private double heading;
	
	public static final double DEFAULT_LONGITUDE = 0.0;
	public static final double DEFAULT_LATITUDE = 0.0;
	public static final double DEFAULT_RANGE = 0.0;
	public static final double DEFAULT_TILT = 0.0;
	public static final double DEFAULT_HEADING = 0.0;

	/**
	 * Constructs a <code>LookAt</code> object with the required attributes 
	 * longitude and latitude.
	 * 
	 * @param longitude the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#longitude">
	 * longitude</a> of the new LookAt object
	 * @param latitude the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#latitude">
	 * latitude</a> of the new LookAt object
	 */
	public LookAt(double longitude, double latitude) {
		
		super("");
		this.longitude = longitude;
		this.latitude = latitude;
		this.range = LookAt.DEFAULT_RANGE;
		this.tilt = LookAt.DEFAULT_TILT;
		this.heading = LookAt.DEFAULT_HEADING;
		
	}
	
	/**
	 * Constructs a <code>LookAt</code> object with all attributes.
	 * 
	 * @param longitude the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#longitude">
	 * longitude</a> of the new LookAt object
	 * @param latitude the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#latitude">
	 * latitude</a> of the new LookAt object
	 * @param range the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#range">
	 * range</a> of the new LookAt object
	 * @param tilt the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#tilt">
	 * tilt</a> of the new LookAt object
	 * @param heading the 
	 * <a href="http://earth.google.com/kml/kml_tags.html#heading">
	 * heading</a> of the new LookAt object
	 */
	public LookAt(double longitude, double latitude, double range, double tilt, double heading) {
		
		super("");
		this.longitude = longitude;
		this.latitude = latitude;
		this.range = range;
		this.tilt = tilt;
		this.heading = heading;
		
	}
	
	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, final String offsetString) throws IOException {
		
		out.write(Object.getOffset(offset, offsetString));
		out.write("<LookAt>");
		out.newLine();
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<longitude>" + this.longitude + "</longitude>");
		out.newLine();
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<latitude>" + this.latitude + "</latitude>");
		out.newLine();
		if (this.range != LookAt.DEFAULT_RANGE) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<range>" + this.range + "</range>");
			out.newLine();
		}
		if (this.tilt != LookAt.DEFAULT_TILT) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<tilt>" + this.tilt + "</tilt>");
			out.newLine();
		}
		if (this.heading != LookAt.DEFAULT_HEADING) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<heading>" + this.heading + "</heading>");
			out.newLine();
		}
		out.write(Object.getOffset(offset, offsetString));
		out.write("</LookAt>");
		out.newLine();

	}
	
}
