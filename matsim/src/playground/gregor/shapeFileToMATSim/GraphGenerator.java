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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author laemmel
 * 
 */
public class GraphGenerator {

	private static final Logger log = Logger.getLogger(GraphGenerator.class);

	private final FeatureSource featureSource;

	private HashSet<LineString> lineStrings;
	private QuadTree<LineString> tree;

	private GeometryFactory geofac;

	public GraphGenerator(FeatureSource featureSource) {
		this.geofac = new GeometryFactory();
		this.featureSource = featureSource;

	}

	public Collection<Feature> createGraph() throws Exception {
		parseLineStrings();
		cleanUpLineStrings();
		checkForIntersectionsToSplit();
		simplifyNetwork();
		mergeNodes();

		return genFeatureCollection();
	}

	private void cleanUpLineStrings() {
		log.info("removing redundant points ...");
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
		log.info("done.");
	}

	private void simplifyNetwork() {
		log.info("unifying fragmented links...");
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
					remove(ls);
					remove(neighbor);
					LineString union = catAtTouchPoint(ls, neighbor);
					add(union);
					lineStringsQueue.add(union);
					break;
				}

			}

		}
		log.info("done.");
	}



	private void checkForIntersectionsToSplit() {
		log.info("check if some LineStrings have to be splitted at intersections ...");
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
				remove(ls);
				splitLineString(ls, splitPoints);
			}
		}
		log.info("done.");

	}


	private void mergeNodes() {
		log.info("");
	
		
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>(this.lineStrings);
		Iterator<LineString> it = lineStringsQueue.iterator();
		while (it.hasNext()) {
			LineString ls = it.next();
			
			Point [] points = new Point[]{ls.getStartPoint(), ls.getEndPoint()};
			
			for(int i = 0 ; i < points.length ; i++){
			
				Point p = points[i];
				Collection<LineString> lsList = tree.get(p.getX(), p.getY(), GISToMatsimConverter.CATCH_RADIUS);
				for (LineString l : lsList){
					if(l.getStartPoint().equalsExact(p, GISToMatsimConverter.CATCH_RADIUS)){
						if(!l.getStartPoint().equalsExact(p)){
							Coordinate [] c = l.getCoordinates();
							c[0] = p.getCoordinate(); 
							CoordinateSequence seq = new CoordinateArraySequence(c);
							LineString newLine = new LineString(seq, geofac);
							remove(l);
							add(newLine);
						}
					} else if(l.getEndPoint().equalsExact(p, GISToMatsimConverter.CATCH_RADIUS)){
						if(!l.getEndPoint().equalsExact(p)){
							Coordinate [] c = l.getCoordinates();
							c[c.length-1] = p.getCoordinate(); 
							CoordinateSequence seq = new CoordinateArraySequence(c);
							LineString newLine = new LineString(seq, geofac);
							remove(l);
							add(newLine);
						}
					}
				}
			}
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
		int ID = 0;
		for (LineString ls : this.lineStrings){
			Feature ft = ftRoad.create(new Object [] {new MultiLineString(new LineString []{ls},this.geofac) , ID++, -1,-1},"network");
			features.add(ft);
				
		}


		return features;
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

	
	private LineString catAtTouchPoint(LineString ls1, LineString ls2) {

		Coordinate[] carray = new Coordinate[ls1.getNumPoints() + ls2.getNumPoints() - 1];
		boolean reverseLs = false;
		boolean reverseNeighbor = false;
		if (ls1.getStartPoint().equalsExact(ls2.getStartPoint(),
				GISToMatsimConverter.CATCH_RADIUS)
				|| ls1.getStartPoint().equalsExact(ls2.getEndPoint(),
						GISToMatsimConverter.CATCH_RADIUS)) {
			reverseLs = true;
			if (ls1.getStartPoint().equalsExact(ls2.getEndPoint(),
					GISToMatsimConverter.CATCH_RADIUS)) {
				reverseNeighbor = true;
			}
		} else {
			if (ls1.getEndPoint().equalsExact(ls2.getEndPoint(),
					GISToMatsimConverter.CATCH_RADIUS)) {
				reverseNeighbor = true;
			}
		}
		
		if (reverseLs) {
			int j = 0;
			for (int i = ls1.getNumPoints() - 1; i >= 0; i--) {
				carray[j++] = ls1.getCoordinateN(i);
			}
		} else {
			for (int i = 0; i < ls1.getNumPoints(); i++) {
				carray[i] = ls1.getCoordinateN(i);
			}
		}

		int offset = ls1.getNumPoints() - 1;
		if (reverseNeighbor) {
			int j = offset + 1;
			for (int i = ls2.getNumPoints() - 2; i >= 0; i--) {
				carray[j++] = ls2.getCoordinateN(i);
			}
		} else {
			for (int i = 1; i < ls2.getNumPoints(); i++) {
				carray[i + offset] = ls2.getCoordinateN(i);
			}
		}

		return this.geofac.createLineString(carray);

	}

	private void splitLineString(LineString ls, Vector<Point> splitPoints) {
		
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
	
}
