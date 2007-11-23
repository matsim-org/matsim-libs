/* *********************************************************************** *
 * project: org.matsim.*
 * LineString.java
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
import java.util.ArrayList;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://earth.google.com/kml/kml_tags_21.html#linestring">
 * http://earth.google.com/kml/kml_tags_21.html#linestring</a>
 */
public class LineString extends Geometry {


	private ArrayList<Point> points;
	
	/**
	 * Constructs a new <code>LineString</code> object using 
	 * two <code>Point</code> objects.
	 * 
	 * @param fromPoint
	 * the one end of the line
	 * @param toPoint
	 * the other end of the line
	 */
	public LineString(Point fromPoint, Point toPoint) {
		super();
		this.points = new ArrayList<Point>();
		this.points.add(fromPoint);
		this.points.add(toPoint);
	}

	
	/**
	 * Constructs a new <code>LineString</code> object using 
	 * an <code>ArrayList<Point></code> object.
	 * 
	 * @param points
	 * an ArrayList of <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Point</a>  
	 */
	public LineString(ArrayList<Point> points) {
		super();
		this.points = points;
	}

	
	/**
	 * Constructs a new <code>LineString</code> object using 
	 * two <code>Point</code> objects, and user-defined geometry attributes.
	 * 
	 * @param fromPoint
	 * the one end <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Point</a> of the line
	 * @param toPoint
	 * the other end <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Point</a> of the line
	 * @param extrude
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#extrude">
	 * extrude</a> property of the new line string.
	 * @param tessellate
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#tessellate">
	 * tessellate</a> property of the new line string.
	 * @param altitudeMode
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#altitudemode">
	 * altitude mode</a> property of the new line string.
	 */
	public LineString(Point fromPoint, Point toPoint, boolean extrude, boolean tessellate, AltitudeMode altitudeMode) {
		
		super(extrude, tessellate, altitudeMode);
		this.points.add(fromPoint);
		this.points.add(toPoint);
		
	}
	
	
	/**
	 * Constructs a new <code>LineString</code> object using 
	 * an <code>ArrayList<Point></code> object, and user-defined geometry attributes.
	 * 
	 * @param points
	 * An ArrayList of <a href="http://earth.google.com/kml/kml_tags.html#point">
	 * Points
	 * @param extrude
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#extrude">
	 * extrude</a> property of the new line string.
	 * @param tessellate
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#tessellate">
	 * tessellate</a> property of the new line string.
	 * @param altitudeMode
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#altitudemode">
	 * altitude mode</a> property of the new line string.
	 */
	public LineString(ArrayList<Point> points, boolean extrude, boolean tessellate, AltitudeMode altitudeMode) {
		
		super(extrude, tessellate, altitudeMode);
		this.points = points;
		
	}
	
	
	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<LineString>");
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);
		
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<coordinates>");
//		out.newLine();
		
		for (Point point : this.points){
			out.write(Object.getOffset(offset + 2, offsetString));
			out.write(
					point.getLongitude() + "," +
					point.getLatitude() + "," +
					point.getAltitude()				
					);
			out.newLine();			
		}

		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("</coordinates>");
		out.newLine();
		
		out.write(Object.getOffset(offset, offsetString));
		out.write("</LineString>");
		out.newLine();
			
	}	
	
}
