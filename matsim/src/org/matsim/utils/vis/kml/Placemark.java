/* *********************************************************************** *
 * project: org.matsim.*
 * Placemark.java
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
 * <a href="http://earth.google.com/kml/kml_tags_21.html#placemark">
 * http://earth.google.com/kml/kml_tags_21.html#placemark</a>
 *
 */
public class Placemark extends Feature {

	private Geometry geometry;

	public static final Geometry DEFAULT_GEOMETRY = null;

	/**
	 * Constructs a placemark with default values
	 * of its {@link Feature} attributes.
	 *
	 * @param id The id allows unique identification of a KML element.
	 */
	public Placemark(final String id) {

		super(
				id,
				Feature.DEFAULT_NAME,
				Feature.DEFAULT_DESCRIPTION,
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE
				);

		this.geometry = Placemark.DEFAULT_GEOMETRY;

	}

	/**
	 * Constructs a placemark with user-defined values
	 * of its {@link Feature} attributes.
	 *
	 * @param id
	 * the object id of the new placemark
	 * @param name
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#name">
	 * name</a> of the new placemark.
	 * @param description
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#description">
	 * description</a> of the new placemark.
	 * @param lookAt
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#lookat">
	 * lookAt</a> property of the new placemark.
	 * @param styleUrl
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#styleurl">
	 * style URL</a> of the new placemark.
	 * @param visibility
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#visibility">
	 * visibility</a> of the new placemark.
	 * @param region
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#region">
	 * region</a> of the new placemark.
	 * @param timePrimitive
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#timeprimitive">
	 * time primitive</a> of the new placemark.
	 */
	public Placemark(
			final String id,
			final String name,
			final String description,
			final String address,
			final LookAt lookAt,
			final String styleUrl,
			final boolean visibility,
			final Region region,
			final TimePrimitive timePrimitive) {

		super(
				id,
				name,
				description,
				address,
				lookAt,
				styleUrl,
				visibility,
				region,
				timePrimitive);
		this.geometry = Placemark.DEFAULT_GEOMETRY;

	}


	/**
	 * Sets the geometry ({@link Point}, {@link LineString} etc.) of this placemark.
	 *
	 * @param geometry new geometry of this placemark
	 */
	public void setGeometry(final Geometry geometry) {
		this.geometry = geometry;
	}


	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Placemark>");
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);

		if (this.geometry != Placemark.DEFAULT_GEOMETRY) {
			this.geometry.writeObject(out, version, offset + 1, offsetString);
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</Placemark>");
		out.newLine();
		out.newLine();

		out.flush();

	}

}
