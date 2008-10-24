/* *********************************************************************** *
 * project: org.matsim.*
 * Document.java
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
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#document">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#document</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class Document extends Container {

	public Document(String id) {

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

	}

	public Document(
			String id,
			String name,
			String description,
			String address,
			LookAt lookAt,
			String styleUrl,
			boolean visibility,
			Region region,
			TimePrimitive timePrimitive) {

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
	protected void writeObject(BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		out.write("<Document>");
		out.newLine();

		super.writeObject(out, version, (offset + 1), offsetString);

		out.write("</Document>");
		out.newLine();

	}

}
