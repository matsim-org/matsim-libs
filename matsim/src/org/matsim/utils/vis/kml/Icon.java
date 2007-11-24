/* *********************************************************************** *
 * project: org.matsim.*
 * Icon.java
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

/**
 * For documentation, refer to
 * <a href="http://earth.google.com/kml/kml_tags_21.html#icon">
 * http://earth.google.com/kml/kml_tags_21.html#icon</a>
 */
public class Icon extends Object {

	private String href;
	private int x;
	private int y;
	private int w;
	private int h;
	
	// I'm not sure with these default values, I didn't try them out.
	public static final int DEFAULT_X = 0;
	public static final int DEFAULT_Y = 0;
	public static final int DEFAULT_W = 0;
	public static final int DEFAULT_H = 0;
	
	/**
	 * Constructs an icon with the required href attribute.
	 * 
	 * @param href
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#href">
	 * href</a> attribute of the new icon
	 */
	public Icon(String href) {

		super("");
		if (href == null) {
			Gbl.errorMsg("href must not be null.");
		}
		this.href = href;
		this.x = Icon.DEFAULT_X;
		this.y = Icon.DEFAULT_Y;
		this.w = Icon.DEFAULT_W;
		this.h = Icon.DEFAULT_H;
	
	}

	/**
	 * Constructs an icon with all attributes.
	 * 
	 * @param href
	 * the <a href="http://earth.google.com/kml/kml_tags.html#href">
	 * href</a> attribute of the new icon
	 * @param x
	 * the <a href="http://earth.google.com/kml/kml_tags.html#x">
	 * x</a> attribute of the new icon
	 * @param y
	 * the <a href="http://earth.google.com/kml/kml_tags.html#y">
	 * y</a> attribute of the new icon
	 * @param w
	 * the <a href="http://earth.google.com/kml/kml_tags.html#w">
	 * w</a> attribute of the new icon
	 * @param h
	 * the <a href="http://earth.google.com/kml/kml_tags.html#h">
	 * h</a> attribute of the new icon
	 * 
	 * @deprecated since KML version 2.1
	 */
	@Deprecated
	public Icon(String href, int x, int y, int w, int h) {
		
		super("");
		if (href == null) {
			Gbl.errorMsg("href must not be null.");
		}
		this.href = href;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Icon>");
		out.newLine();
		
		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<href>" + this.href + "</href>");
		out.newLine();

		if (version == XMLNS.V_20) {
			if (this.x != Icon.DEFAULT_X) {
				out.write(Object.getOffset(offset + 1, offsetString));
				out.write("<x>" + this.x + "</x>");
				out.newLine();
			}
			if (this.y != Icon.DEFAULT_Y) {
				out.write(Object.getOffset(offset + 1, offsetString));
				out.write("<y>" + this.y + "</y>");
				out.newLine();
			}
			if (this.w != Icon.DEFAULT_W) {
				out.write(Object.getOffset(offset + 1, offsetString));
				out.write("<w>" + this.w + "</w>");
				out.newLine();
			}
			if (this.h != Icon.DEFAULT_H) {
				out.write(Object.getOffset(offset + 1, offsetString));
				out.write("<h>" + this.h + "</h>");
				out.newLine();
			}
		}		
		out.write(Object.getOffset(offset, offsetString));
		out.write("</Icon>");
		out.newLine();

	}
	
	
}
