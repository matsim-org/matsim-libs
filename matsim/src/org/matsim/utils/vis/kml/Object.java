/* *********************************************************************** *
 * project: org.matsim.*
 * Object.java
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
 * <a href="http://earth.google.com/kml/kml_tags_21.html#object">
 * http://earth.google.com/kml/kml_tags_21.html#object</a>
 */
public abstract class Object {

	private String id;

	public Object(String id) {
		super();
		this.id = id;
	}

	/**
	 * Sets the id of this object.
	 *
	 * @param id new object id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the id of this object
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Each class representing a KML element provides this method to define
	 * how it is written into a KML file. The writing procedure is nested,
	 * that is each class representing a KML element calls the writeObject
	 * method for the elements it may contain.
	 *
	 * @param out Specifies where the object is written.
	 * @param version Specifies the version of the KML language. It may
	 * depend on the version how an element is written, e.g. if tags are
	 * deprecated from one version to the other.
	 * @param offset A counter specifying the offset in the KML file. Usually,
	 * the string offsetString is written offset times before the content is
	 * written.
	 * @param offsetString The string representing one offset level for the
	 * KML file (e.g. space or tab).
	 * @throws IOException
	 */
	protected abstract void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException;

	/**
	 * Concatenates the string offsetString offset times in order to produce
	 * a offset string to be written to a KML/XML file.
	 *
	 * @param offset the offset counter
	 * @param offsetString the string to be multiplied
	 * @return the concatenated string
	 * @throws IOException
	 */
	protected static String getOffset(final int offset, final String offsetString) {

		String str = "";

		for (int ii=0; ii<offset; ii++) {
			str = str.concat(offsetString);
		}

		return str;
	}

}
