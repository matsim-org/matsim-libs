/* *********************************************************************** *
 * project: org.matsim.*
 * Geometry.java
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
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#geometry">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#geometry</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public abstract class Geometry extends Object {

	public enum AltitudeMode {
	
		CLAMP_TO_GROUND ("clampToGround"), 
		RELATIVE_TO_GROUND ("relativeToGround"), 
		ABSOLUTE ("absolute");
		
		private String strAltitudeMode;

		private AltitudeMode(String strAltitudeMode) {
			this.strAltitudeMode = strAltitudeMode;
		}

		@Override
		public String toString() {
			return this.strAltitudeMode;
		}
		
	};

	private boolean extrude;
	private boolean tessellate;
	private AltitudeMode altitudeMode;
	
	public static final boolean DEFAULT_EXTRUDE = false;
	public static final boolean DEFAULT_TESSELLATE = false;
	public static final AltitudeMode DEFAULT_ALTITUDE_MODE = AltitudeMode.CLAMP_TO_GROUND;
	
	/**
	 * Construts the geometry with default values of its attributes.
	 */
	protected Geometry() {
		
		super("");
		this.extrude = Geometry.DEFAULT_EXTRUDE;
		this.tessellate = Geometry.DEFAULT_TESSELLATE;
		this.altitudeMode = Geometry.DEFAULT_ALTITUDE_MODE;
		
	}

	/**
	 * Constructs the geometry with user-defined values of its attributes.
	 * 
	 * @param extrude
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#extrude">
	 * extrude</a> property of the new geometry.
	 * @param tessellate
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#tessellate">
	 * tessellate</a> property of the new geometry.
	 * @param altitudeMode
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#altitudemode">
	 * altitude mode</a> property of the new geometry.
	 */
	protected Geometry(boolean extrude, boolean tessellate, AltitudeMode altitudeMode) {
		
		super("");
		this.extrude = extrude;
		this.tessellate = tessellate;
		this.altitudeMode = altitudeMode;
		
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {
		
		if (this.extrude != Geometry.DEFAULT_EXTRUDE) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<extrude>1</extrude>");
			out.newLine();
		}

		if (this.tessellate != Geometry.DEFAULT_TESSELLATE) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<tessellate>1</tessellate>");
			out.newLine();
		}

		if (this.altitudeMode != Geometry.DEFAULT_ALTITUDE_MODE) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<altitudeMode>" + this.altitudeMode.toString() + "</altitudeMode>");
			out.newLine();
		
		}
		
	}

}
