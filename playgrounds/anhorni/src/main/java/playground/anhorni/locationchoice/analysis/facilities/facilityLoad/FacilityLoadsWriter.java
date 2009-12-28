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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Point;

public class FacilityLoadsWriter {

	private FeatureType featureType;
	
	public void write(TreeMap<Id, FacilityLoad> facilityLoads) {
				
		this.initGeometries();
		String shopFileIncreased = "output/postprocessing/shopLoads_Increased.shp";
		String shopFileDecreased = "output/postprocessing/shopLoads_Decreased.shp";
		
		ArrayList<Feature> featuresIncreased = new ArrayList<Feature>();
		ArrayList<Feature> featuresDecreased = new ArrayList<Feature>();
		
		for (FacilityLoad facilityLoad : facilityLoads.values()) {
			
				Feature feature = this.createFeature(
						facilityLoad.getCoord(), facilityLoad.getFacilityId(), facilityLoad.getLoadDiffRel());
				
				if (facilityLoad.getLoadDiffRel() > 0.8) {
					featuresIncreased.add(feature);
				}
				else if (facilityLoad.getLoadDiffRel() < - 0.8) {
					featuresDecreased.add(feature);
				}
		}
		try {
			if (featuresIncreased.size() > 0) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)featuresIncreased, shopFileIncreased);
			}
			if (featuresDecreased.size() > 0) { 
				ShapeFileWriter.writeGeometries((Collection<Feature>)featuresDecreased, shopFileDecreased);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	private void initGeometries() {
		AttributeType [] attr = new AttributeType[3];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attr[2] = AttributeTypeFactory.newAttributeType("loadDiff", String.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord, IdImpl id, double loadDiff ) {		
		Feature feature = null;
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord), id.toString(), Double.toString(loadDiff)});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
}
