/* *********************************************************************** *
 * project: org.matsim.*
 * BufferHull.java
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
package playground.gregor.gis.bufferhull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.convexer.Concaver;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;

public class BufferHull {

	
	private final String in;
	private final String out;
	private FeatureType featureType;
	private CoordinateReferenceSystem crs;

	public BufferHull(String in, String out) {
		this.in = in;
		this.out = out;
	}


	private void run() throws IOException, FactoryRegistryException, SchemaException, IllegalAttributeException {
		initFeatures();
		FeatureSource fs = ShapeFileReader.readDataFile(this.in);
		this.crs = fs.getSchema().getDefaultGeometry().getCoordinateSystem();
		Iterator it = fs.getFeatures().iterator();
		GeometryFactory geofac = new GeometryFactory();
		List<Coordinate> coords = new ArrayList<Coordinate>();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			if (coords.size() == 22474) {
				int ii = 0;
				ii++;
			}
			Geometry geo = ft.getDefaultGeometry();
			Geometry geo2 = geo;
			Coordinate[] cs = geo2.getCoordinates();
			for (int i = 0 ; i < cs.length; i++) {
				coords.add(cs[i]);
			}
			System.out.println(coords.size());
		}
		System.out.println("creating multipoint");
		Coordinate [] mpcs = new Coordinate[coords.size()];
		for (int i = 0; i < coords.size(); i++) {
			mpcs[i] = coords.get(i);
		}
		MultiPoint mp = geofac.createMultiPoint(mpcs);
		System.out.println("concave hull");
		Concaver cc = new Concaver();
		Polygon hull = cc.getConcaveHull(mp);
		Collection<Feature> fts = new ArrayList<Feature>();
		fts.add(this.featureType.create(new Object[]{hull,1}));
				
	}
	
	private void initFeatures() throws FactoryRegistryException, SchemaException {
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, this.crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[] { geom, id}, "buffer");
	}
	
	public static void main(String [] args) {
		String in = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/inundation/20100201_sz_pc_2b_tide_subsidence/sz_pc_2b_tide_subsidence_2010_01_28_max_inundation.shp";
		String out = "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/buffer.shp";
		try {
			new BufferHull(in,out).run();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FactoryRegistryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
