/* *********************************************************************** *
 * project: org.matsim.*
 * Style.java
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
 * <a href="http://earth.google.com/kml/kml_tags_21.html#style">
 * http://earth.google.com/kml/kml_tags_21.html#style</a>
 */
public class Style extends Object {

	private IconStyle iconStyle;
	private LineStyle lineStyle;
	private PolyStyle polyStyle;
	private LabelStyle labelStyle;
	
	public static final IconStyle DEFAULT_ICON_STYLE = null;
	public static final LineStyle DEFAULT_LINE_STYLE = null;
	public static final PolyStyle DEFAULT_POLY_STYLE = null;
	public static final LabelStyle DEFAULT_LABEL_STYLE = null;

	/**
	 * Constructs an empty style with default attribute values.
	 *
	 * @param id
	 * the style ID.
	 */
	public Style(String id) {

		super(id);
		this.iconStyle = Style.DEFAULT_ICON_STYLE;
		this.lineStyle = Style.DEFAULT_LINE_STYLE;
		this.polyStyle = Style.DEFAULT_POLY_STYLE;
		this.labelStyle = Style.DEFAULT_LABEL_STYLE;
		
	}

	/**
	 * Sets the <a href="http://earth.google.com/kml/kml_tags.html#iconstyle">
	 * icon style</a> of a <code>Style</code>.
	 *
	 * @param iconStyle
	 */
	public void setIconStyle(final IconStyle iconStyle) {

		this.iconStyle = iconStyle;

	}

	/**
	 * Sets the <a href="http://earth.google.com/kml/kml_tags.html#linestyle">
	 * line style</a> of a <code>Style</code>.
	 *
	 * @param lineStyle
	 */
	public void setLineStyle(LineStyle lineStyle) {

		this.lineStyle = lineStyle;

	}

	/**
	 * Sets the <a href="http://earth.google.com/kml/kml_tags.html#polystyle">
	 * poly style</a> of a <code>Style</code>.
	 *
	 * @param polyStyle
	 */
	public void setPolyStyle(PolyStyle polyStyle) {

		this.polyStyle = polyStyle;

	}

	
	
	/**
	 * Sets the <a href="http://earth.google.com/kml/kml_tags.html#labelstyle">
	 * label style</a> of a <code>Style</code>.
	 *
	 * @param labelStyle
	 */
	public void setLabelStyle(LabelStyle labelStyle) {
		this.labelStyle = labelStyle;
	}

	/**
	 * For documentation, refer to
	 * <a href="http://earth.google.com/kml/kml_tags_21.html#styleurl">
	 * http://earth.google.com/kml/kml_tags_21.html#styleurl</a>
	 * @return the style-url for this style
	 */
	public String getStyleUrl() {
		return "#" + this.getId();
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Style id=\"" + this.getId() + "\">");
		out.newLine();

		if (this.iconStyle != Style.DEFAULT_ICON_STYLE) {
			this.iconStyle.writeObject(out, version, (offset + 1), offsetString);
		}
		if (this.lineStyle != Style.DEFAULT_LINE_STYLE) {
			this.lineStyle.writeObject(out, version, (offset + 1), offsetString);
		}
		if (this.polyStyle != Style.DEFAULT_POLY_STYLE) {
			this.polyStyle.writeObject(out, version, (offset + 1), offsetString);
		}
		if (this.labelStyle != Style.DEFAULT_LABEL_STYLE) {
			this.labelStyle.writeObject(out, version, (offset + 1), offsetString);
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</Style>");
		out.newLine();
		out.newLine();

	}

}
