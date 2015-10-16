/* *********************************************************************** *
 * project: org.matsim.*
 * DynamicSpatialGrid.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.gis;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.opengis.feature.simple.SimpleFeature;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.GraphBuilder;
import playground.johannes.sna.graph.spatial.SpatialVertex;
import playground.johannes.sna.graph.spatial.io.ColorUtils;
import playground.johannes.sna.graph.spatial.io.Colorizable;
import playground.johannes.socialnetworks.gis.io.FeatureKMLWriter;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialFilter;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author illenberger
 * 
 */
public class DynamicSpatialGrid {

	private static final GeometryFactory factory = new GeometryFactory();

	private Node<List<Point>> root;
	
	public DynamicSpatialGrid(Set<Point> points, int minSize) {
		Envelope env = PointUtils.envelope(points);
		root = new Node<List<Point>>();
		root.envelope = env;
		root.value = new ArrayList<Point>(points);
		root.parent = null;
		split(root, minSize);
	}

	public Node getRoot() {
		return root;
	}
	
	private void split(Node<List<Point>> node, int minSize) {
		if (node.value.size() > minSize) {
			node.children = new Node[2];
			double x = node.envelope.getMinX();
			double y = node.envelope.getMinY();
			double dx = node.envelope.getWidth();
			double dy = node.envelope.getHeight();

			Envelope e1;
			Envelope e2;

			if (dx > dy) {
				// split vertically
				e1 = new Envelope(x, x + dx / 2.0, y, y + dy);
				e2 = new Envelope(x + dx / 2.0, x + dx, y, y + dy);
			} else {
				// split horizontally
				e1 = new Envelope(x, x + dx, y, y + dy / 2.0);
				e2 = new Envelope(x, x + dx, y + dy / 2.0, y + dy);
			}
			
			Node<List<Point>> n1 = new Node<List<Point>>();
			n1.value = new ArrayList<Point>(node.value.size());
			n1.parent = node;
			n1.envelope = e1;

			Node<List<Point>> n2 = new Node<List<Point>>();
			n2.value = new ArrayList<Point>(node.value.size());
			n2.parent = node;
			n2.envelope = e2;

			for (Point p : node.value) {
				if (e1.contains(p.getCoordinate())) {
					n1.value.add(p);
				} else if (e2.contains(p.getCoordinate())) {
					n2.value.add(p);
				} else {
					throw new RuntimeException("Dunno where to put this point.");
				}
			}

			node.children[0] = n1;
			node.children[1] = n2;

			if (dx > 1000 && dx > 1000) {
				for (Node<List<Point>> n : node.children)
					split(n, minSize);
			}

		}
	}

	public double getValue(Node<List<Point>> n) {
		return n.value.size()/((n.envelope.getWidth() * n.envelope.getHeight()))*10000;
	}
	
	public Set<Node> getNodes() {
		Set<Node> nodes = new HashSet<DynamicSpatialGrid.Node>();
//		nodes.add(root);
		addNodes(nodes, root);
		return nodes;
	}
	
	private void addNodes(Set<Node> nodes, Node n) {
		if (n.children != null) {
			if (n.children[0] != null) {
				// nodes.add(n.children[0]);
				addNodes(nodes, n.children[0]);
			}
			if (n.children[1] != null) {
				// nodes.add(n.children[1]);
				addNodes(nodes, n.children[1]);
			}
		} else {
			nodes.add(n);
		}
			
	}
	private class Node<T> {

		Envelope envelope;

		T value;

		Node parent;

		Node[] children;
	}
	
	public static void writeKML(DynamicSpatialGrid grid, String file) {
		Set<Geometry> geometries = new HashSet<Geometry>();
		Map<Object, Node> map = new HashMap<Object, DynamicSpatialGrid.Node>();
		TObjectDoubleHashMap values = new TObjectDoubleHashMap();
		for(Node n : grid.getNodes()) {
			Coordinate[] coords = new Coordinate[5];
			coords[0] = new Coordinate(n.envelope.getMinX(), n.envelope.getMinY());
			coords[1] = new Coordinate(n.envelope.getMinX(), n.envelope.getMaxY());
			coords[2] = new Coordinate(n.envelope.getMaxX(), n.envelope.getMaxY());
			coords[3] = new Coordinate(n.envelope.getMaxX(), n.envelope.getMinY());
			coords[4] = new Coordinate(n.envelope.getMinX(), n.envelope.getMinY());
			LinearRing shell = factory.createLinearRing(coords);
			shell.setSRID(21781);
//			Geometry g = factory.createPolygon(shell, null);
			geometries.add(shell);
			
			map.put(shell, n);
			values.put(shell, grid.getValue(n));
		}
		
		NumericAttributeColorizer colorizer = new NumericAttributeColorizer(values);
		colorizer.setLogscale(true);
		FeatureKMLWriter writer = new FeatureKMLWriter();
//		writer.setColorizable(new Colorizer(map, grid));
		writer.setColorizable(colorizer);
		writer.write(geometries, file);
	}
	
	private static class Colorizer implements Colorizable {

		private double norm;
		
		private Map<Object, Node> map;
		
		private DynamicSpatialGrid grid;
		
		public Colorizer(Map<Object, Node> map, DynamicSpatialGrid grid) {
			this.map = map;
			this.grid = grid;
			
			double min = Double.MAX_VALUE;
			double max = 0;
			for(Node n : map.values()) {
				double val = grid.getValue(n);
				norm += val;
				min = Math.min(min, val);
				max = Math.max(max, val);
			}
			
			System.out.println("Min = " + min + ", max = " + max);
		}
		@Override
		public Color getColor(Object object) {
			Node n = map.get(object);
			double val = grid.getValue(n)/norm;
			System.out.println(String.valueOf(grid.getValue(n)));
			return ColorUtils.getGRBColor(val);
		}
		
	}

	public static void main(String args[]) throws IOException {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		
		SimpleFeature feature = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry chBorder = (Geometry) feature.getDefaultGeometry();
		chBorder.setSRID(21781);
		
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
		SpatialFilter filter = new SpatialFilter((GraphBuilder) builder, chBorder);
		graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) filter.apply(graph);
		
		Set<Point> points = new HashSet<Point>();
		for(SpatialVertex v : graph.getVertices()) {
			points.add(v.getPoint());
		}
		
		System.out.println("Creaing grid...");
		DynamicSpatialGrid grid = new DynamicSpatialGrid(points, 50);
		DynamicSpatialGrid.writeKML(grid, "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/density.kml");
	}
	
}
