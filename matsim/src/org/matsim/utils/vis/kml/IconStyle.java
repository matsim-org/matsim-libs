/* *********************************************************************** *
 * project: org.matsim.*
 * IconStyle.java
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

/**
 * For documentation, refer to
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#iconstyle">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#iconstyle</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class IconStyle extends ColorStyle {

	private double scale;
	private Icon icon;

	public static final double DEFAULT_SCALE = 1.0;

	/**
	 * Constructs an icon style with a user-defined Icon object,
	 * and default attribute values.
	 *
	 * @param icon
	 * the <a href="http://earth.google.com/kml/kml_tags.html#icon">
	 * icon</a> of the new icon style
	 */
	public IconStyle(final Icon icon) {

		super(Color.DEFAULT_COLOR, ColorStyle.DEFAULT_COLOR_MODE);
		if (icon == null) {
			Gbl.errorMsg("Icon must not be null.");
		}
		this.icon = icon;
		this.scale = IconStyle.DEFAULT_SCALE;
	}

	/**
	 * Constructs an icon style with a user-defined icon and user-defined
	 * attributes.
	 *
	 * @param icon
	 * the <a href="http://earth.google.com/kml/kml_tags.html#icon">
	 * icon</a> of the icon in the icon style
	 * @param color
	 * the <a href="http://earth.google.com/kml/kml_tags.html#color">
	 * color</a> of the icon in the icon style
	 * @param colorMode
	 * the <a href="http://earth.google.com/kml/kml_tags.html#colormode">
	 * color mode</a> of the icon in the icon style
	 * @param scale
	 * the <a href="http://earth.google.com/kml/kml_tags.html#scale">
	 * scale</a> of the icon in the icon style
	 */
	public IconStyle(final Icon icon, final Color color, final ColorMode colorMode, final double scale) {

		super(color, colorMode);
		if (icon == null) {
			Gbl.errorMsg("Icon must not be null.");
		}
		this.icon = icon;
		this.scale = scale;
	}

	/**
	 * @return
	 * the <a href="http://earth.google.com/kml/kml_tags.html#scale">
	 * scale</a> of the icon style
	 */
	public double getScale() {
		return this.scale;
	}

	/**
	 * @return
	 * the <a href="http://earth.google.com/kml/kml_tags.html#icon">
	 * Icon object</a> of the icon style
	 */
	public Icon getIcon() {
		return this.icon;
	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<IconStyle>");
		out.newLine();

		super.writeObject(out, version, (offset + 1), offsetString);

		this.icon.writeObject(out, version, offset + 1, offsetString);

		if (this.scale != IconStyle.DEFAULT_SCALE) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<scale>" + this.scale + "</scale>");
			out.newLine();
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</IconStyle>");
		out.newLine();

	}


}
