/* *********************************************************************** *
 * project: org.matsim.*
 * ColorStyle.java
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

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.kml.KMLWriter.XMLNS;
import org.matsim.utils.vis.kml.fields.Color;

public abstract class ColorStyle extends Object {

	public enum ColorMode {

		NORMAL ("normal"),
		RANDOM ("random");

		private String strColorMode;

		private ColorMode(final String strColorMode) {
			this.strColorMode = strColorMode;
		}

		@Override
		public String toString() {
			return this.strColorMode;
		}

	};

	protected Color color;
	protected ColorMode colorMode;

	

	public static final ColorMode DEFAULT_COLOR_MODE = ColorMode.NORMAL;

	public ColorStyle(final Color color, final ColorMode colorMode) {

		super("");
		if (color == null) {
			Gbl.errorMsg("Color must not be null.");
		}
		this.color = color;
		if (colorMode == null) {
			Gbl.errorMsg("Color mode must not be null.");
		}
		this.colorMode = colorMode;

	}

	/**
	 * @return
	 * the <a href="http://earth.google.com/kml/kml_tags.html#color">
	 * color</a> of the icon style
	 */
	public Color getColor() {
		return this.color;
	}

	/**
	 * @return
	 * the <a href="http://earth.google.com/kml/kml_tags.html#color">
	 * color mode</a> of the icon style
	 */
	public ColorMode getColorMode() {
		return this.colorMode;
	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		if (!(Color.DEFAULT_COLOR.equals(this.color))) {
			out.write(Object.getOffset(offset, offsetString));
			out.write(this.color.toString());
			out.newLine();
		}

		if (this.colorMode != ColorStyle.DEFAULT_COLOR_MODE) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<colorMode>");
			out.write(this.colorMode.toString());
			out.write("</colorMode>");
			out.newLine();
		}

	}

}
