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

package playground.gregor.gis.featurefilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class StringMatchBasedFilter {
	
	public static void main(String [] args) {
		String in = "/Users/laemmel/devel/pt_evac_demo/raw_input/hamburg_points.shp";
		String out = "/Users/laemmel/devel/pt_evac_demo/raw_input/points_filtered.shp";
		
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(in);
		Collection<SimpleFeature> fts = r.getFeatureSet();
		Iterator<SimpleFeature> it = fts.iterator();
		List<SimpleFeature> ff = new ArrayList<SimpleFeature>();
		while (it.hasNext()) {
			SimpleFeature ft = it.next();
			String tags = (String) ft.getAttribute("tags");
			if (tags.contains("shelter") && tags.contains("bus_stop") && !tags.contains("\"shelter\"=\"no\"")) {
				String t2 = tags.toLowerCase();
				try {
					ft.setAttribute("tags","BB");
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				ff.add(ft);
				System.out.println(tags);
				
			}
		}
		ShapeFileWriter.writeGeometries(ff,out);
	}

}
