/* *********************************************************************** *
 * project: org.matsim.*
 * Folder.java
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
 * <a href="http://earth.google.com/kml/kml_tags.html#folder">
 * http://earth.google.com/kml/kml_tags.html#folder</a>
 *
 * @author meisterk
 *
 */
public class Folder extends Container {

	/**
	 * Constructs a folder with default attribute values.
	 *
	 * @param id The id allows unique identification of a KML element.
	 */
	public Folder(final String id) {
		super(id);
	}

	/**
	 * Constructs a folder with user-defined attribute values.
	 *
	 * @param id The id allows unique identification of a KML element.
	 * @param name
	 * the <a href="http://earth.google.com/kml/kml_tags.html#name">
	 * name</a> of the new folder.
	 * @param description
	 * the <a href="http://earth.google.com/kml/kml_tags.html#desription">
	 * description</a> of the new folder.
	 * @param lookAt
	 * @param styleUrl
	 * @param visibility
	 * the <a href="http://earth.google.com/kml/kml_tags.html#visibility">
	 * visibility</a> of the new folder.
	 * @param region
	 * @param timePrimitive
	 */
	public Folder(
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

	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Folder>");
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);

		out.write(Object.getOffset(offset, offsetString));
		out.write("</Folder>");
		out.newLine();
		out.newLine();

	}


}
