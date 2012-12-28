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

package playground.ciarif.roadpricing;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;

public class ShapeFilePolygonWriter {
	protected GeometryFactory geofac;
	private CoordinateReferenceSystem crs;

	public ShapeFilePolygonWriter() {
		this.geofac = new GeometryFactory();
	}

	public void writePolygon(Coord[] coords, String outfile) {
		Collection<SimpleFeature> features = getFeature(coords);

		ShapeFileWriter.writeGeometries(features, outfile);
	}

	public Collection<SimpleFeature> getFeature(Coord[] coordinates) {
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().
				setCrs(this.crs).
				setName("polygon").
				create();
	
		SimpleFeature polygon = factory.createPolygon(coordinates);

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		features.add(polygon);
		return features;
	}
}
