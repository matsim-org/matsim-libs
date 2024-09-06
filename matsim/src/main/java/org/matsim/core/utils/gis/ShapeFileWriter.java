/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileWriter.java
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

package org.matsim.core.utils.gis;

import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.core.api.internal.MatsimSomeWriter;

import java.util.Collection;

/**
 * This is a simple utility class that provides methods to write Feature instances
 * of the geotools framework to an ESRI shape file.
 *
 * @author glaemmel
 */
@Deprecated
public class ShapeFileWriter implements MatsimSomeWriter {

	public static void writeGeometries(final Collection<SimpleFeature> features, final String filename) {
		GeoFileWriter.writeGeometries(features, filename);
	}
}
