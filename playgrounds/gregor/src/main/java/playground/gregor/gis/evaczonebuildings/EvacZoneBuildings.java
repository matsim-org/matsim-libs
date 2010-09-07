/* *********************************************************************** *
 * project: org.matsim.*
 * EvacZoneBuildings.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.gis.evaczonebuildings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Point;

public class EvacZoneBuildings {

	
	public static void main(String [] args) throws IOException {
		
		String evacZone = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/evac_zone/zone_10_20m_buffer.shp";
		String in = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/buildings_v20100315/evac_zone_buildings_v20100315.shp";
		String out = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/buildings_v20100315/evac_zone_buildings_v20100315.shp";
	
		FeatureSource fs1 = ShapeFileReader.readDataFile(evacZone);
		Iterator it1 = fs1.getFeatures().iterator();
		List<Feature> zone = new ArrayList<Feature>();
		while (it1.hasNext()) {
			Feature ft1 = (Feature) it1.next();
			zone.add(ft1);
		}
		
		
		Collection<Feature> fts = new ArrayList<Feature>();
		FeatureSource fs2 = ShapeFileReader.readDataFile(in);
		Iterator it2 = fs2.getFeatures().iterator();
		
		int ns = fs2.getFeatures().size();
		int count = 0;
		while (it2.hasNext()) {
			count ++;
			if (count % 100 == 0) {
				System.out.println("done:" + count + " of" + ns);
			}
			Feature ft = (Feature) it2.next();
			Point centro = ft.getDefaultGeometry().getCentroid();
			for (Feature ft1 : zone) {
				if (ft1.getDefaultGeometry().contains(centro)) {
					fts.add(ft);
					break;
				}
			}
			
		}
		ShapeFileWriter.writeGeometries(fts, out);
		
	}
}
