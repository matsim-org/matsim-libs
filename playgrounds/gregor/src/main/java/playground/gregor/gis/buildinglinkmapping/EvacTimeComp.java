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

package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class EvacTimeComp {
	
	public static void main(String [] args) {
		String r1994 = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings_b.shp";
		String r1992 = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings.shp";
		
		ShapeFileReader reader94 = new ShapeFileReader();
		ShapeFileReader reader92 = new ShapeFileReader();
		reader94.readFileAndInitialize(r1994);
		reader92.readFileAndInitialize(r1992);
		Map<String,SimpleFeature> r92m = new HashMap<String, SimpleFeature>();
		for (SimpleFeature ft : reader92.getFeatureSet()) {
			String name = (String) ft.getAttribute("name");
			r92m.put(name, ft);
		}
		
		Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		for (SimpleFeature ft : reader94.getFeatureSet()) {
			String name = (String) ft.getAttribute("name");
			SimpleFeature ft2 = r92m.get(name);
			Double t94 = (Double) ft.getAttribute("dblAvgZ");
			Double t92 = (Double) ft2.getAttribute("dblAvgZ");
			double diff = t94-t92;
			ft.setAttribute("dblAvgZ", diff);
			fts.add(ft);
		}
		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings_diff.shp");
	}

}
