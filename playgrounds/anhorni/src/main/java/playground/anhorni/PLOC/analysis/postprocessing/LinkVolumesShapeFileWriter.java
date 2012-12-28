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

package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class LinkVolumesShapeFileWriter {
	
	public void writeLinkVolumesAtCountStations(String outpath, List<LinkWInfo> links, int hour) {		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
			.addAttribute("ID", String.class)
			.addAttribute("stdDev", Double.class)
			.addAttribute("avgVolume", Double.class)
			.create();
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();	

		for (LinkWInfo link : links) {
			SimpleFeature feature = factory.createPoint(
					MGC.coord2Coordinate(link.getCoord()), 
					new Object[] {link.getId().toString(), link.getStdDevs(hour), link.getAvgVolume(hour)}, 
					link.getId().toString());
			features.add(feature);
		}
		
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries(features, outpath  + "/volumes_" + hour + ".shp");
		}
	}
		
}
