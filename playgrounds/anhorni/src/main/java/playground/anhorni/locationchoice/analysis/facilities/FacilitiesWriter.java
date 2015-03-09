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

package playground.anhorni.locationchoice.analysis.facilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

public class FacilitiesWriter {

	private SimpleFeatureBuilder builder;

	public int[] write(List<ActivityFacility> facilities)  {

		this.initGeometries();
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		int numberOfShops[] = {0,0,0,0,0,0};

		Iterator<ActivityFacility> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ActivityFacility facility = facilities_it.next();
			Coord coord = facility.getCoord();


			if (facility.getActivityOptions().get("shop_retail_gt2500sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_gt2500sqm"));
				numberOfShops[0]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_get1000sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_get1000sqm"));
				numberOfShops[1]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_get400sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_get400sqm"));
				numberOfShops[2]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_get100sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_get100sqm"));
				numberOfShops[3]++;
			}
			else if (facility.getActivityOptions().get("shop_retail_lt100sqm") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_retail_lt100sqm"));
				numberOfShops[4]++;
			}
			else if (facility.getActivityOptions().get("shop_other") != null) {
				features.add(this.createFeature(coord, facility.getId(), "shop_other"));
				numberOfShops[5]++;
			}
		}
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries(features, "output/zhFacilities.shp");
		}
		return numberOfShops;
	}


	private void initGeometries() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("point");
		b.add("location", Point.class);
		b.add("ID", String.class);
		b.add("Type", String.class);
		this.builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}

	private SimpleFeature createFeature(Coord coord, Id id, String type) {
		return this.builder.buildFeature(id.toString(), new Object [] {MGC.coord2Point(coord),  id.toString(), type});
	}

}
