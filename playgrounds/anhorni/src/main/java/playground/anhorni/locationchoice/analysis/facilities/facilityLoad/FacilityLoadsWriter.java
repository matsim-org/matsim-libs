/* *********************************************************************** *
 * project: org.matsim.*
 * PrintShopAndLeisureLocations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import java.util.ArrayList;
import java.util.TreeMap;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

public class FacilityLoadsWriter {

	private SimpleFeatureBuilder builder;
	
	public void write(TreeMap<Id<ActivityFacility>, FacilityLoad> facilityLoads) {
		this.initGeometries();
		String shopFileIncreased = "output/postprocessing/shopLoads_Increased.shp";
		String shopFileDecreased = "output/postprocessing/shopLoads_Decreased.shp";
		
		ArrayList<SimpleFeature> featuresIncreased = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresDecreased = new ArrayList<SimpleFeature>();
		
		for (FacilityLoad facilityLoad : facilityLoads.values()) {
			SimpleFeature feature = this.createFeature(
					facilityLoad.getCoord(), facilityLoad.getFacilityId(), facilityLoad.getLoadDiffRel());
			
			if (facilityLoad.getLoadDiffRel() > 0.8) {
				featuresIncreased.add(feature);
			}
			else if (facilityLoad.getLoadDiffRel() < -0.8) {
				featuresDecreased.add(feature);
			}
		}
		if (featuresIncreased.size() > 0) {
			ShapeFileWriter.writeGeometries(featuresIncreased, shopFileIncreased);
		}
		if (featuresDecreased.size() > 0) { 
			ShapeFileWriter.writeGeometries(featuresDecreased, shopFileDecreased);
		}
	}
		
	private void initGeometries() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("point");
		b.add("location", Point.class);
		b.add("ID", String.class);
		b.add("loadDiff", String.class);
		this.builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private SimpleFeature createFeature(Coord coord, Id<ActivityFacility> id, double loadDiff ) {
		return this.builder.buildFeature(id.toString(), new Object [] {MGC.coord2Point(coord), id.toString(), Double.toString(loadDiff)});
	}
}
