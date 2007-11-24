/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkLink.java
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
 * <a href="http://earth.google.com/kml/kml_tags_21.html#networklink">
 * http://earth.google.com/kml/kml_tags_21.html#networklink</a>
 *
 */
public class NetworkLink extends Feature {

	private final Link link;

	/**
	 * Constructs a network link with default values
	 * of its {@link Feature} attributes.
	 *
	 * @param id The id allows unique identification of a KML element.
	 * @param link
	 * the location of the referenced KML/KMZ file
	 */
	public NetworkLink(final String id, final Link link) {
		super(id);
		this.link = link;
	}

	/**
	 * Constructs a network link with user-defined values
	 * of its {@link Feature} attributes.
	 *
	 * @param id The id allows unique identification of a KML element.
	 * @param link
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#link">
	 * link</a> of the new network link.
	 * @param name
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#name">
	 * name</a> of the new network link.
	 * @param description
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#description">
	 * description</a> of the new network link.
	 * @param address
	 * @param lookAt
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#lookat">
	 * lookAt</a> property of the new network link.
	 * @param styleUrl
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#styleurl">
	 * style URL</a> of the new network link.
	 * @param visibility
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#visibility">
	 * visibility</a> of the new network link.
	 * @param region
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#region">
	 * region</a> of the new network link.
	 * @param timePrimitive
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#timeprimitive">
	 * time primitive</a> of the new network link.
	 */
	public NetworkLink(
			final String id,
			final Link link,
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
		this.link = link;

	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<NetworkLink>");
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);

		this.link.writeObject(out, version, offset + 1, offsetString);

		out.write(Object.getOffset(offset, offsetString));
		out.write("</NetworkLink>");
		out.newLine();

	}

}
