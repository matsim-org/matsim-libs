/* *********************************************************************** *
 * project: org.matsim.*
 * PolyStyle.java
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
import org.matsim.utils.vis.kml.fields.Color;

/**
 * For documentation, refer to
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#polystyle">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#polystyle</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class PolyStyle extends ColorStyle {

	private boolean fill;
	private boolean outline;
	
	public static final boolean FILL_TRUE = true;
	public static final boolean FILL_FALSE = false;
	public static final boolean OUTLINE_TRUE = true;
	public static final boolean OUTLINE_FALSE = false;
	
	public static final boolean DEFAULT_FILL = PolyStyle.FILL_TRUE;
	public static final boolean DEFAULT_OUTLINE = PolyStyle.OUTLINE_TRUE;
	
	public PolyStyle() {
		
		super(Color.DEFAULT_COLOR, ColorStyle.DEFAULT_COLOR_MODE);
		this.fill = PolyStyle.DEFAULT_FILL;
		this.outline = PolyStyle.DEFAULT_OUTLINE;
		
	}

	public PolyStyle(final Color color, final ColorMode colorMode, final boolean fill, final boolean outline) {
		
		super(color, colorMode);
		this.fill = fill;
		this.outline = outline;
		
	}
	
	public boolean isFill() {
		return this.fill;
	}
	public boolean isOutline() {
		return this.outline;
	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {
		out.write(Object.getOffset(offset, offsetString));
		out.write("<PolyStyle>");
		out.newLine();
		
		super.writeObject(out, version, offset + 1, offsetString);

		if (this.fill != PolyStyle.DEFAULT_FILL) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<fill>" + this.fill + "</fill>");
			out.newLine();
		}

		if (this.outline != PolyStyle.DEFAULT_OUTLINE) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<outline>" + this.outline + "</outline>");
			out.newLine();
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</PolyStyle>");
		out.newLine();
	}

	
}
