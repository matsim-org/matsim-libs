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

package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class BuildingLinkMapper {

	public static void main(String [] args) {
		String conf = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/config.xml";
		String runBase = "/Users/laemmel/svn/runs-svn/run1390/";
		Config c = ConfigUtils.loadConfig(conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		Loader loader = new Loader(sc);
		loader.loadData();
		List<Building> buildings = loader.getBuildings();
		System.out.println(buildings.size());
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize("/Users/laemmel/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/links_v20090728.shp");
		QuadTree<SimpleFeature> quad = buildQuadTree(reader);
		
		Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		
		PolygonFeatureFactory factory = initFeatures();
		for (Building b : buildings) {
			Collection<SimpleFeature> coll = quad.get(b.getGeo().getCentroid().getX(), b.getGeo().getCentroid().getY(), 100);
			if (coll.size() == 0) {
				coll = quad.get(b.getGeo().getCentroid().getX(), b.getGeo().getCentroid().getY(), 500);
			} 
			
			Set<SimpleFeature> killed = new HashSet<SimpleFeature>();
			double minDist = Double.POSITIVE_INFINITY;
			SimpleFeature nearest = null;
			for (SimpleFeature ft : coll) {
				
				
				if (killed.contains(ft)) {
					continue;
				}
				
				killed.add(ft);
				
				long intId = (Long)ft.getAttribute("ID");
				Id lId = new IdImpl(intId);
				if (sc.getNetwork().getLinks().get(lId) == null) {
					continue;
				}
				
				double dist = b.getGeo().distance((Geometry) ft.getDefaultGeometry());
				if (dist < minDist) {
					minDist = dist;
					nearest = ft;
				}
			}
			if (nearest == null) {
				System.out.println("skiped: " + b.getPopDay());
				continue;
			}
			int intId = Integer.parseInt(b.getId().toString());
			int qp = b.isQuakeProof() ? 1 : 0;
			if (b.getGeo() instanceof Polygon) {
				fts.add(factory.createPolygon((Polygon) b.getGeo(), new Object[]{intId,b.getPopNight(),b.getPopDay(),b.getPopAf(),b.getFloor(),b.getShelterSpace(),qp,b.getMinWidth(),nearest.getAttribute("ID")}, null));
			} else {
				fts.add(factory.createPolygon((MultiPolygon) b.getGeo(), new Object[]{intId,b.getPopNight(),b.getPopDay(),b.getPopAf(),b.getFloor(),b.getShelterSpace(),qp,b.getMinWidth(),nearest.getAttribute("ID")}, null));
			}
			if (fts.size() % 100 == 0) {
				System.out.println(fts.size());
			}
		}
		
//		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/svn/shared-svn/studies/countries/id/padang/network/evac_zone_buildings_v20120206.shp");
		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/tmp/aaaa.shp");
		
	}

	private static PolygonFeatureFactory initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32747");
		return new PolygonFeatureFactory.Builder().
				setCrs(targetCRS).
				addAttribute("ID", Integer.class).
				addAttribute("popNight", Integer.class).
				addAttribute("popDay", Integer.class).
				addAttribute("popAf", Integer.class).
				addAttribute("floor", Integer.class).
				addAttribute("capacity", Integer.class).
				addAttribute("quakeProof", Integer.class).
				addAttribute("minWidth", Double.class).
				addAttribute("linkId", Integer.class).
				create();
	}
	
	private static QuadTree<SimpleFeature> buildQuadTree(ShapeFileReader reader) {
		Envelope e = reader.getBounds();
		QuadTree<SimpleFeature> quad = new QuadTree<SimpleFeature>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		for (SimpleFeature ft : reader.getFeatureSet()) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			for (int i = 0; i < geo.getCoordinates().length; i++) {
				Coordinate c = geo.getCoordinates()[i];
				quad.put(c.x, c.y, ft);
			}
			
		}
		return quad;
	}
}
