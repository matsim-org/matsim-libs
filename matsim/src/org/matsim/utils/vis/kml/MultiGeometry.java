/* *********************************************************************** *
 * project: org.matsim.*
 * MultiGeometry.java
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
import java.util.LinkedList;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://earth.google.com/kml/kml_tags_21.html#multigeometry">
 * http://earth.google.com/kml/kml_tags_21.html#multigeometry</a><br>
 * <br>
 * A multigeometry is a container for one or more geometry primitives 
 * associated with the same feature. It allows the grouping of features, so
 * the user doesn't see all the single parts that build this complex feature.
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class MultiGeometry extends Geometry {
	
	private LinkedList<Geometry> geometries = new LinkedList<Geometry>();
	
	public MultiGeometry() {
	}
	
	public void addGeometry(Geometry geometry) {
		this.geometries.add(geometry);
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset, String offsetString) throws IOException {
		out.write(Object.getOffset(offset, offsetString));
		out.write("<MultiGeometry>");
		out.newLine();

		super.writeObject(out, version, offset + 1, offsetString);
		out.newLine();
		
		for (Geometry geometry : this.geometries) {
			geometry.writeObject(out, version, offset + 1, offsetString);
		}
		
		out.write(Object.getOffset(offset, offsetString));
		out.write("</MultiGeometry>");
		out.newLine();
	}

}
