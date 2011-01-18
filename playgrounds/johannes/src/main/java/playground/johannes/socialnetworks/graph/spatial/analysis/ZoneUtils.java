/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneUtils.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import com.vividsolutions.jts.geom.Geometry;

import playground.johannes.socialnetworks.gis.io.FeatureKMLWriter;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.studies.gis.Accessibility;

/**
 * @author illenberger
 *
 */
public class ZoneUtils {

	public static <V extends SpatialVertex> ZoneLayer<Set<V>> fillZoneLayer(ZoneLayer<Set<V>> layer, Set<V> vertices) {
		for(V v : vertices) {
			Zone<Set<V>> zone = layer.getZone(v.getPoint());
			if(zone != null) {
				Set<V> set = zone.getAttribute();
				if(set == null) {
					set = new HashSet<V>();
					zone.setAttribute(set);
				}
				set.add(v);
			}
		}
		
		return layer;
	}
	
	public static void main(String args[]) throws IOException {
		SpatialGraph g = new SpatialGraphMLReader().readGraph("/Users/jillenberger/Work/socialnets/mcmc/output-switch/2100000000/graph.graphml");
		
//		ZoneLayer<Set<SpatialVertex>> layer =  ZoneLayerSHP.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1G08.shp");
//		layer.overwriteCRS(CRSUtils.getCRS(21781));
		
		Geometry boundary = FeatureSHP.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next().getDefaultGeometry();
		boundary.setSRID(21781);
		
		ZoneLayer<Set<SpatialVertex>> layer = Accessibility.createGridLayer(10000, boundary);
		
		ZoneUtils.fillZoneLayer(layer, (Set<SpatialVertex>)g.getVertices());
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		
		TObjectDoubleHashMap<Geometry> colors = new TObjectDoubleHashMap<Geometry>();
		Set<Geometry> geometries = new HashSet<Geometry>();
		for(Zone<Set<SpatialVertex>> zone : layer.getZones()) {
			if(zone.getAttribute() != null) {
			double k = Degree.getInstance().distribution(zone.getAttribute()).getMean();
			colors.put(zone.getGeometry(), k);
			geometries.add(zone.getGeometry());
			}
		}
		
		NumericAttributeColorizer colorizer = new NumericAttributeColorizer(colors);
		colorizer.setLogscale(true);
		writer.setColorizable(colorizer);
		
		writer.write(geometries, "/Users/jillenberger/Work/socialnets/mcmc/output-switch/2100000000/zones.kmz");
	}
}
