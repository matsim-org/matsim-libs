/* *********************************************************************** *
 * project: org.matsim.*
 * Union.java
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
package playground.gregor.gis.union;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Union {

	
	private String links;
	private String nodes;

	
	private static final double minX = 0;
	private static final double maxX = Double.POSITIVE_INFINITY;
	private static final double minY = 0;
	private static final double maxY = Double.POSITIVE_INFINITY;
	private Geometry geo = null;
	
	private final Map<Long,List<Feature>> linksMapping = new HashMap<Long,List<Feature>>();
	private final Map<Long,Feature> nodesMapping = new HashMap<Long, Feature>();
	private final Set<Feature> closed = new HashSet<Feature>();
	private ArrayList<Feature> features;
	private FeatureType ftRunCompare;
		
	public Union(String links, String nodes) {
		this.links = links;
		this.nodes = nodes;
	}

	
	private void run() {
		try {
			readLinks();
			readNode();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Queue<Long> openNodes = new ConcurrentLinkedQueue<Long>();
		openNodes.add(new Long(1));
		
		
int iters = 0;
		while (openNodes.size() > 0) {
			Long current = openNodes.poll();
			Feature ft = this.nodesMapping.get(current);
			if (this.closed.contains(ft)) {
				continue;
			}
			
			Geometry node = ft.getDefaultGeometry();
			double x = node.getCoordinate().x;
			double y = node.getCoordinate().y;
			
			if (x > maxX || x < minX || y > maxY || y < minY) {
				continue;
			}
			
			
			node = node.buffer(0.51);
			
			if (this.geo == null) {
				this.geo = node;
			} else {
				this.geo = node.union(this.geo);
			}
			exploreNode(openNodes,current);			
			this.closed.add(ft);
			
			if (iters++ % 50 == 0) {
				System.out.println("processed nodes = " + iters);
			}
//			if (iters >= 600) {
//				openNodes.clear();
//			}
		}
		
		initFeatures();
		try {
			this.features.add(this.ftRunCompare.create(new Object[]{this.geo,"HaHa"}));
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		try {
			ShapeFileWriter.writeGeometries(this.features, "/home/laemmel/devel/sim2d/data/duisburg/duisburg.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}	
	
	
	
	
	private void exploreNode(Queue<Long> openNodes, Long current) {
		List<Feature> ll = this.linksMapping.get(current);
		for (Feature ft : ll) {
			if (this.closed.contains(ft)) {
				continue;
			}
			this.geo = ft.getDefaultGeometry().union(this.geo);
			this.closed.add(ft);
			openNodes.add((Long) ft.getAttribute("fromID"));
			openNodes.add((Long) ft.getAttribute("toID"));
			
			
		}
		
	}


	private void readNode() throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(nodes);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature)it.next();
			Long id = (Long) ft.getAttribute("ID");
			this.nodesMapping.put(id, ft);
			
		}
	}


	private void readLinks() throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(links);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature)it.next();
			Long from = (Long)ft.getAttribute("fromID");
			List<Feature> fl = this.linksMapping.get(from);
			if (fl == null) {
				fl = new ArrayList<Feature>();
				this.linksMapping.put(from, fl);
			}
			fl.add(ft);
			Long to = (Long)ft.getAttribute("toID");
			List<Feature> tl = this.linksMapping.get(to);
			if (tl == null) {
				tl = new ArrayList<Feature>();
				this.linksMapping.put(to, tl);
			}
			tl.add(ft);
		}
	}


	private void initFeatures() {
		this.features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG: 32632");
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",Polygon.class, true, null, null, crs);
		AttributeType risk = AttributeTypeFactory.newAttributeType("LANDUSE", String.class);
		
		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, risk}, "LANDUSE");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

	}
	
	
	public static void main(String [] args) {
		String links = "/home/laemmel/devel/sim2d/data/duisburg/links.shp";
		String nodes = "/home/laemmel/devel/sim2d/data/duisburg/nodes.shp";
		new Union(links,nodes).run();

	}


	
}
