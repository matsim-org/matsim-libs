/* *********************************************************************** *
 * project: org.matsim.*
 * Region.java
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
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#region">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#region</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class Region extends Object {

	private double north;
	private double south;
	private double west;
	private double east;
	
	private int minLodPixels;
	private int maxLodPixels;
	
	public static final int DEFAULT_MIN_LOD_PIXELS = 0;
	public static final int DEFAULT_MAX_LOD_PIXELS = -1;
	
	/**
	 * Constructs a <code>Region</code> with the required attributes,
	 * and default values for the optional attributes.
	 * 
	 * @param north 
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#north">
	 * north</a> attribute of the new region.
	 * @param south
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#south">
	 * south</a> attribute of the new region.
	 * @param west
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#west">
	 * west</a> attribute of the new region.
	 * @param east
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#east">
	 * east</a> attribute of the new region.
	 */
	public Region(double north, double south, double west, double east) {
		super("");
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
		this.minLodPixels = Region.DEFAULT_MIN_LOD_PIXELS;
		this.maxLodPixels = Region.DEFAULT_MAX_LOD_PIXELS;
	}

	/**
	 * Constructs a <code>Region</code> with all attributes.
	 * 
	 * @param north 
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#north">
	 * north</a> attribute of the new region.
	 * @param south
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#south">
	 * south</a> attribute of the new region.
	 * @param west
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#west">
	 * west</a> attribute of the new region.
	 * @param east
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#east">
	 * east</a> attribute of the new region.
	 * @param minLodPixels
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#minlodpixels">
	 * minLodPixels</a> attribute of the new region.
	 * @param maxLodPixels
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#maxlodpixels">
	 * maxLodPixels</a> attribute of the new region.
	 */
	public Region(double north, double south, double west, double east, int minLodPixels, int maxLodPixels) {
		super("");
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
		this.minLodPixels = minLodPixels;
		this.maxLodPixels = maxLodPixels;
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset,
			String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Region>");
		out.newLine();
		
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<LatLonAltBox>");
		out.newLine();

		out.write(Object.getOffset(offset + 2, offsetString));
		out.write("<north>" + this.north + "</north>");
		out.newLine();
		out.write(Object.getOffset(offset + 2, offsetString));
		out.write("<south>" + this.south + "</south>");
		out.newLine();
		out.write(Object.getOffset(offset + 2, offsetString));
		out.write("<west>" + this.west + "</west>");
		out.newLine();
		out.write(Object.getOffset(offset + 2, offsetString));
		out.write("<east>" + this.east + "</east>");
		out.newLine();
		
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("</LatLonAltBox>");
		out.newLine();

		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<Lod>");
		out.newLine();

		if (this.minLodPixels != Region.DEFAULT_MIN_LOD_PIXELS) {
			out.write(Object.getOffset(offset + 2, offsetString));
			out.write("<minLodPixels>" + this.minLodPixels + "</minLodPixels>");
			out.newLine();
		}
		
		if (this.maxLodPixels != Region.DEFAULT_MAX_LOD_PIXELS) {
			out.write(Object.getOffset(offset + 2, offsetString));
			out.write("<maxLodPixels>" + this.maxLodPixels + "</maxLodPixels>");
			out.newLine();
		}
		
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("</Lod>");
		out.newLine();

		out.write(Object.getOffset(offset, offsetString));
		out.write("</Region>");
		out.newLine();
		
	
	}

}
