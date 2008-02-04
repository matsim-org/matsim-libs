/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkBuild.java
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

/**
 * 
 */
package playground.gregor.shapeFileToMATSim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultFeatureResults;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.CollectionDataStore;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;

import org.geotools.referencing.CRS;
import org.matsim.counts.AttributeFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.collections.QuadTree;

import playground.gregor.gis.MedianStripShapeFileWriter.PFeatureCollection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author laemmel
 * 
 */
public class GraphGenerator {

	private static final Logger log = Logger.getLogger(GraphGenerator.class);

	private NetworkLayer network = null;
	private final FeatureSource featureSource;

	private HashSet<LineString> lineStrings;
	private QuadTree<LineString> tree;

	private GeometryFactory geofac;

	// DEBUG
	ShapefileDataStore ds = null;

	private String outfile = "./padang/debug_out.shp";

	public GraphGenerator(FeatureSource featureSource) {
		this.geofac = new GeometryFactory();
		this.network = new NetworkLayer();
		this.featureSource = featureSource;

	}

	public Collection<Feature> createNetwork() throws Exception {
		parseLineStrings();
		cleanUpLineStrings();
		checkForIntersectionsToSplit();
		simplifyNetwork();

		return genFeatureCollection();
	}

	private void cleanUpLineStrings() {
		Iterator<LineString> it = this.lineStrings.iterator();
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()) {
			lineStringsQueue.add((LineString) it.next());
		}
		while (lineStringsQueue.peek() != null) {
			LineString ls = lineStringsQueue.poll();
			Point p = ls.getStartPoint();
			Vector<Coordinate> coords = new Vector<Coordinate>();
			coords.add(p.getCoordinate());
			for (int i = 1; i < ls.getNumPoints(); i++) {
				Point tmp = ls.getPointN(i);
				if (p.distance(ls.getPointN(i)) <= GISToMatsimConverter.CATCH_RADIUS) {
					if (tmp.equals(ls.getEndPoint())) {
						coords.remove(coords.lastElement());
						coords.add(tmp.getCoordinate());
					}
				} else {
					coords.add(tmp.getCoordinate());
				}
				p = tmp;
			}
			if (coords.size() < ls.getNumPoints()) {
				remove(ls);
				Coordinate[] carray = new Coordinate[coords.size()];
				for (int i = 0; i < coords.size(); i++) {
					carray[i] = coords.elementAt(i);
				}
				LineString cleanLs = this.geofac.createLineString(carray);
				add(cleanLs);
			}
		}
	}

	private void simplifyNetwork() {
		Iterator<LineString> it = this.lineStrings.iterator();
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()) {
			lineStringsQueue.add((LineString) it.next());
		}
		while (lineStringsQueue.peek() != null) {
			LineString ls = lineStringsQueue.poll();
			Collection<LineString> tmp = this.tree.get(ls.getStartPoint()
					.getX(), ls.getStartPoint().getY(), GISToMatsimConverter.CATCH_RADIUS);
			if (tmp.size() != 2) {
				tmp = this.tree.get(ls.getEndPoint().getX(), ls.getEndPoint()
						.getY(), GISToMatsimConverter.CATCH_RADIUS);
			}
			if (tmp.size() == 2) {
				for (LineString neighbor : tmp) {
					if (neighbor.equals(ls)) {
						continue;
					}
					union(ls, neighbor, lineStringsQueue);
					break;
				}

			}

		}

	}

	private void union(LineString ls, LineString neighbor,
			ConcurrentLinkedQueue<LineString> lineStringsQueue) {
		remove(ls);
		remove(neighbor);
		Coordinate[] carray = new Coordinate[ls.getNumPoints()
				+ neighbor.getNumPoints() - 1];
		boolean reverseLs = false;
		boolean reverseNeighbor = false;
		if (ls.getStartPoint().equalsExact(neighbor.getStartPoint(),
				GISToMatsimConverter.CATCH_RADIUS)
				|| ls.getStartPoint().equalsExact(neighbor.getEndPoint(),
						GISToMatsimConverter.CATCH_RADIUS)) {
			reverseLs = true;
			if (ls.getStartPoint().equalsExact(neighbor.getEndPoint(),
					GISToMatsimConverter.CATCH_RADIUS)) {
				reverseNeighbor = true;
			}
		} else {
			if (ls.getEndPoint().equalsExact(neighbor.getEndPoint(),
					GISToMatsimConverter.CATCH_RADIUS)) {
				reverseNeighbor = true;
			}
		}

		if (reverseLs) {
			int j = 0;
			for (int i = ls.getNumPoints() - 1; i >= 0; i--) {
				carray[j++] = ls.getCoordinateN(i);
			}
		} else {
			for (int i = 0; i < ls.getNumPoints(); i++) {
				carray[i] = ls.getCoordinateN(i);
			}
		}

		int offset = ls.getNumPoints() - 1;
		if (reverseNeighbor) {
			int j = offset + 1;
			for (int i = neighbor.getNumPoints() - 2; i >= 0; i--) {
				carray[j++] = neighbor.getCoordinateN(i);
			}
		} else {
			for (int i = 1; i < neighbor.getNumPoints(); i++) {
				carray[i + offset] = neighbor.getCoordinateN(i);
			}
		}

		LineString union = this.geofac.createLineString(carray);
		add(union);
		lineStringsQueue.add(union);
	}

	private void checkForIntersectionsToSplit() {
		log
				.info("check if some LineStrings have to be splitted at intersections ...");
		Iterator<LineString> it = this.lineStrings.iterator();
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()) {
			lineStringsQueue.add((LineString) it.next());
		}
		while (lineStringsQueue.peek() != null) {
			LineString ls = lineStringsQueue.poll();

			Vector<Point> splitPoints = new Vector<Point>();
			for (int i = 1; i < ls.getNumPoints() - 1; i++) {
				// TODO add public boolean valueExistsAt(x,y,CATCH_RADIUS) to
				// QuadTree
				Collection<LineString> tmp = this.tree.get(
						ls.getCoordinateN(i).x, ls.getCoordinateN(i).y,
						GISToMatsimConverter.CATCH_RADIUS);
				if (!tmp.isEmpty()) {
					splitPoints.add(ls.getPointN(i));
				}
			}
			if (splitPoints.size() > 0) {
				splitLineString(ls, splitPoints);
			}
		}
		log.info("done.");

	}

	private void splitLineString(LineString ls, Vector<Point> splitPoints) {
		remove(ls);
		splitPoints.add(ls.getEndPoint());
		Point tmp = ls.getStartPoint();
		Vector<Coordinate> segment = new Vector<Coordinate>();
		int count = 0;
		for (Point splitPoint : splitPoints) {
			while (!tmp.equals(splitPoint)) {
				segment.add(tmp.getCoordinate());
				tmp = ls.getPointN(count++);
			}
			segment.add(tmp.getCoordinate());
			Coordinate[] coords = new Coordinate[segment.size()];
			for (int j = 0; j < segment.size(); j++) {
				coords[j] = segment.elementAt(j);
			}
			LineString subLs = this.geofac.createLineString(coords);
			add(subLs);
			segment.clear();
		}

	}

	private void parseLineStrings() throws IOException {

		log.info("parsing features and building up QuadTree ...");
		FeatureCollection collection = this.featureSource.getFeatures();
		Envelope o = this.featureSource.getBounds();
		this.lineStrings = new HashSet<LineString>();
		this.tree = new QuadTree<LineString>(o.getMinX(), o.getMinY(), o
				.getMaxX(), o.getMaxY());

		FeatureIterator it = collection.features();
		while (it.hasNext()) {
			Feature feature = it.next();
			MultiLineString multiLineString = (MultiLineString) feature
					.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				LineString lineString = (LineString) multiLineString
						.getGeometryN(i);
				add(lineString);
			}

		}
		log.info("done.");

	}

	private void add(LineString ls) {
		this.lineStrings.add(ls);
		this.tree.put(ls.getStartPoint().getX(), ls.getStartPoint().getY(), ls);
		this.tree.put(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
	}

	private void remove(LineString ls) {
		this.lineStrings.remove(ls);
		this.tree.remove(ls.getStartPoint().getX(), ls.getStartPoint().getY(),
				ls);
		this.tree.remove(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
	}


	private Collection<Feature> genFeatureCollection() throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, this.featureSource.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", Integer.class);
		AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", Integer.class);
		AttributeType toNode = AttributeTypeFactory.newAttributeType(
				"toID", Integer.class);		
		FeatureType ftRoad = FeatureTypeFactory.newFeatureType(
				new AttributeType[] { geom, id, fromNode, toNode }, "link");
		
		for (LineString ls : this.lineStrings){
			Feature ft = ftRoad.create(new Object [] {new MultiLineString(new LineString []{ls},this.geofac) , -1, -1,-1},"network");
			features.add(ft);
				
		}


		return features;
	}

}
