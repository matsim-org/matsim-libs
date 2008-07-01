/* *********************************************************************** *
 * project: org.matsim.*
 * Polygon.java
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
 * For documentation, refer to
 * <a href="http://earth.google.com/kml/kml_tags_21.html#polygon">
 * http://earth.google.com/kml/kml_tags_21.html#polygon</a>
 *
 * @author dgrether
 *
 */
public class Polygon extends Geometry {
	/**
	 * A Polygon has always zero or one Outer-
	 */
	private LinearRing outerBoundarie;
	/**
	 * and zero to many InnerBoundaries.
	 */
	private List<LinearRing> innerBoundaries;
	/**
	 * Creates a Polygon without geometries
	 *
	 */
	public Polygon() {
		super();
		this.innerBoundaries = null;
		this.outerBoundarie = null;

	}
	/**
	 * Creates a Polygon without Geometries but with the given flags
	 * @param extrude
	 * @param tessellate
	 * @param altitudeMode
	 */
	public Polygon(boolean extrude, boolean tessellate, AltitudeMode altitudeMode) {
		this(extrude, tessellate, altitudeMode, null);
	}
	/**
	 * Creates a Polygon without holes, e.g. no inner boundaries.
	 * @param extrude
	 * @param tessellate
	 * @param altitudeMode
	 * @param outerBoundaries
	 */
	public Polygon(boolean extrude, boolean tessellate, AltitudeMode altitudeMode, LinearRing outerBoundaries) {
		super(extrude, tessellate, altitudeMode);
		this.outerBoundarie = outerBoundaries;
		this.innerBoundaries = null;
	}

	public void setBoundary(LinearRing boundary) {
		this.outerBoundarie = boundary;
	}

	/**
	 * Adds a LinearRing object to the innerBoundaries of this Polygon
	 * @param boundary
	 */
	public void addInnerBoundary(LinearRing boundary) {
		if (this.innerBoundaries == null)
			this.innerBoundaries = new Vector<LinearRing>();

		this.innerBoundaries.add(boundary);
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		if ((this.getId() != null) && (this.getId().length() != 0)) {
			out.write("<Polygon id=" + this.getId() + ">");
		}
		else {
			out.write("<Polygon>");
		}
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);

		if (this.outerBoundarie != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<outerBoundaryIs>");
			out.newLine();
			this.outerBoundarie.writeObject(out, version, offset + 2, offsetString);
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("</outerBoundaryIs>");
			out.newLine();
		}

		if (this.innerBoundaries != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<innerBoundaryIs>");
			out.newLine();
			for (LinearRing lr : this.innerBoundaries) {
				lr.writeObject(out, version, offset + 2, offsetString);
			}
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("</innerBoundaryIs>");
			out.newLine();
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</Polygon>");
		out.newLine();
	}
}
