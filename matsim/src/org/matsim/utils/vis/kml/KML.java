/* *********************************************************************** *
 * project: org.matsim.*
 * KML.java
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

public class KML {

	private Feature feature = null;

	public KML() {
		super();
	}

	public Feature getFeature() {
		return this.feature;
	}

	public void setFeature(final Feature feature) {
		this.feature = feature;
	}

	public void writeKML(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString, final XMLNS xmlNS) throws IOException {

		out.write("<kml xmlns=\"");
		out.write(xmlNS.toString());
		out.write("\">");
		out.newLine();

		if (this.feature != null) {
			this.feature.writeObject(out, version, offset, offsetString);
		}

		out.write("</kml>");
		out.newLine();

	}

}
