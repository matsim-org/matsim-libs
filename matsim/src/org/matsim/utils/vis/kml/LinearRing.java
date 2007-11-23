/* *********************************************************************** *
 * project: org.matsim.*
 * LinearRing.java
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
import java.util.List;
import java.util.Vector;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * 
 * For documentation, refer to <a
 * href="http://code.google.com/apis/kml/documentation/kml_tags_21.html#linearring">
 * http://code.google.com/apis/kml/documentation/kml_tags_21.html#linearring</a>
 * 
 * @author dgrether
 * 
 */
public class LinearRing extends Geometry {
	/**
	 * The List containing the coordinates of the LinearRing. Consider that a
	 * Linear Ring should be closed, i.e. first Point == last Point.
	 */
	private List<Point> coordinates;

	/**
   * 
   */
	public LinearRing() {
		super();
	}

	/**
   * @param extrude
   * @param tessellate
   * @param altitudeMode
   */
	public LinearRing(boolean extrude, boolean tessellate,
			AltitudeMode altitudeMode) {
		super(extrude, tessellate, altitudeMode);
	}

	public LinearRing(List<Point> coordinates) {
		super();
		this.coordinates = coordinates;
	}

	/**
   * Adds a Point to the coordinates of this LinearRing. When using this method
   * consider that a ring should be closed, i.e. first Point == last Point.
   * 
   * @param coord
   */
	public void addCoordinate(Point coord) {
		if (this.coordinates == null)
			this.coordinates = new Vector<Point>();

		this.coordinates.add(coord);
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset,
			String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<LinearRing>");
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);
		if (coordinates != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<coordinates>");
			out.newLine();

			for (Point p : coordinates) {
				out.write(Object.getOffset(offset + 2, offsetString));
				out.write(p.getLongitude() + "," + p.getLatitude() + ","
						+ p.getAltitude());
				out.newLine();
			}

			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("</coordinates>");
			out.newLine();
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</LinearRing>");
		out.newLine();

	}
}
