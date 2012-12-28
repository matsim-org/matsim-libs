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

package playground.anhorni.choiceSetGeneration.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;
import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

import com.vividsolutions.jts.geom.Point;


public class ZHFacilitiesWriter {

	private SimpleFeatureBuilder builder;

	public void write(String outdir, ZHFacilities facilities)  {
						
		this.initGeometries();
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();	
		ArrayList<SimpleFeature> featuresExact = new ArrayList<SimpleFeature>();	
		
		Iterator<ZHFacility> facilities_it = facilities.getZhFacilities().values().iterator();
		while (facilities_it.hasNext()) {
			ZHFacility facility = facilities_it.next();
			Coord coord = facility.getMappedPosition();
			features.add(this.createFeature(coord, facility.getId()));
			featuresExact.add(this.createFeature(facility.getExactPosition(), facility.getId()));
		}
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)features, outdir +"/shapefiles/zhFacilitiesPositionMapped2Net.shp");
			ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)featuresExact, outdir +"/shapefiles/zhFacilitiesExactPosition.shp");
		}
	}

	private void initGeometries() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("point");
		b.add("location", Point.class);
		b.add("ID", String.class);
		this.builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private SimpleFeature createFeature(Coord coord, Id id) {
		return this.builder.buildFeature(id.toString(), new Object [] {MGC.coord2Point(coord),  id.toString()});
	}
	
}
