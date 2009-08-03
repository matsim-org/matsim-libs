/* *********************************************************************** *
 * project: org.matsim.*
 * Convexer.java
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

package playground.gregor.gis.convexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class Convexer {
	private final static GeometryFactory geofac = new GeometryFactory();

	private final FeatureSource inFt;
	private final String outfile;
	private final FeatureType ftType;
	private final Collection<Feature> fetures = new ArrayList<Feature>();
	public Convexer(final FeatureSource inFt, final String output){
		this.inFt = inFt;
		this.ftType = this.inFt.getSchema();
		this.outfile = output;

	}

	public void convex() {
		final PseudoConvexDecompositor convexer = new PseudoConvexDecompositor();
		final Collection<Feature> fts = getPolygons(this.inFt);
		int toGo = fts.size();
		for (final Feature ft : fts) {
			if (toGo-- % 100 == 0) {
				System.out.println("toGo:"  + toGo);
			}

			int qp = (Integer) ft.getAttribute("quakeProof");
			if (qp != 1) {
				continue;
			}
			
			final Geometry geo = ft.getDefaultGeometry();
			Polygon poly;
			if (geo instanceof Polygon) {
				poly = (Polygon) geo;
			}  else if (geo instanceof MultiPolygon) {
				poly = (Polygon)((MultiPolygon)geo).getGeometryN(0);
			} else {
				throw new RuntimeException("Feature contains no polygon!");
			}
			final Collection<Polygon> ps = convexer.decompose(poly);
			addPolygons(ps,ft);

		}

		write();
	}

	private void addPolygons(final Collection<Polygon> ps, final Feature ft) {

		for (final Polygon p : ps) {
			Object [] attr = new Object [ft.getNumberOfAttributes()];
			attr = ft.getAttributes(attr);
			final MultiPolygon mp = geofac.createMultiPolygon(new Polygon [] {p});
			attr[0] = mp;
			try {
				this.fetures.add(this.ftType.create(attr,"convex"));
			} catch (final IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void write() {
		try {
			ShapeFileWriter.writeGeometries(this.fetures, this.outfile);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private  Collection<Feature> getPolygons(final FeatureSource n) {
		final Collection<Feature> polygons = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
//			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
//			if (multiPolygon.getNumGeometries() > 1) {
//				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
//			}
//			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			polygons.add(feature);
	}

		return polygons;
	}

	public static void main(final String [] args) throws Exception {
		String input = "../../inputs/networks/evac_zone_buildings_v20090728.shp";
//		final String input = "./tmp/regionII.shp";
		final String output = "../../inputs/gis/shelters.shp";
		final FeatureSource inFt = ShapeFileReader.readDataFile(input);
		new Convexer(inFt,output).convex();

	}

}
