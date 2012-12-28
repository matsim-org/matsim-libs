/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.multidestpeds.helper;

import java.util.Iterator;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class OffenRemover {

	public static void main(String [] args) {
		String in = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed_transformed.shp";
		String out = "/Users/laemmel/devel/dfg/input/boundaries_closed.shp";
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(in);
		Iterator<SimpleFeature> it = r.getFeatureSet().iterator();
		while (it.hasNext()) {
			SimpleFeature ft = it.next();
			if (ft.getAttribute("lines").toString().equals("offen")) {
				it.remove();
			}
		}
		ShapeFileWriter.writeGeometries(r.getFeatureSet(), out);
	}
}
