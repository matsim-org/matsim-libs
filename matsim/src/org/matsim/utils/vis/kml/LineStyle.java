/* *********************************************************************** *
 * project: org.matsim.*
 * LineStyle.java
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
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#linestyle">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#linestyle</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class LineStyle extends ColorStyle {

	private int width;

	public static final int DEFAULT_WIDTH = 1;
	
	public LineStyle() {
		
		super(Color.DEFAULT_COLOR, ColorStyle.DEFAULT_COLOR_MODE);
		this.width = LineStyle.DEFAULT_WIDTH;
		
	}

	public LineStyle(final Color color, final ColorMode colorMode, final int width) {

		super(color, colorMode);
		this.width = width;

	}
	
	public int getWidth() {
		return this.width;
	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {
		out.write(Object.getOffset(offset, offsetString));
		out.write("<LineStyle>");
		out.newLine();
		
		super.writeObject(out, version, offset + 1, offsetString);

		if (this.width != LineStyle.DEFAULT_WIDTH) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<width>" + this.width + "</width>");
			out.newLine();
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</LineStyle>");
		out.newLine();

}
	
	
}
