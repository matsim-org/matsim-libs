/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.gis;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class ShapeFilePolygonWriter {

	protected GeometryFactory geofac;
	private CoordinateReferenceSystem crs;


	public ShapeFilePolygonWriter() {
		this.geofac = new GeometryFactory();
	}


	public void writePolygon(Coord[] coords, String outfile) {

		Collection<SimpleFeature> features = getFeature(getLinearRing(coords));


		ShapeFileWriter.writeGeometries(features, outfile);
	}

	public LinearRing getLinearRing(Coord[] coords) {
		Coordinate[] coordinates = new Coordinate[coords.length];
		for (int i = 0; i < coords.length; i++) {
			coordinates[i] = new Coordinate(coords[i].getX(), coords[i].getY());
		}
		return new LinearRing(new CoordinateArraySequence(coordinates), this.geofac);
	}

	public Collection<SimpleFeature> getFeature(LinearRing ring) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(this.crs);
		b.setName("polygon");
		b.add("location", MultiPolygon.class);
		
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		
		Polygon p = new Polygon(ring, null, this.geofac);
		MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);
		SimpleFeature f = builder.buildFeature(null, new Object[] { mp });
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		features.add(f);
		return features;
	}

}
