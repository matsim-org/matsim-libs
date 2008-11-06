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
package playground.gregor.southafrica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Level;
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
 * GraphGenerator kann aus einem beliebigen *.shp-File von LineStrings die 
 * Netzwerkstruktur extrahieren (z.B. als Input für den NetworkGenerator). Das 
 * ist natürlich etwas komplizierter, da erohne from-to Information auskommt. 
 * Ausserdem werden redundante Knoten (Knoten an denen sich genau zwei 
 * LineStrings treffen) entfernt und noch einige weitere Optimierungen 
 * vorgenommen.
 * 
 * @author laemmel
 * 
 */
public class GraphGenerator {

	private static final Logger log = Logger.getLogger(GraphGenerator.class);

	private final FeatureSource featureSource;

	private HashSet<LineString> lineStrings;
	private QuadTree<LineString> tree;
	private QuadTree<Node> nodes;

	private final GeometryFactory geofac;
	
	private final static double MIN_LENGTH = 20;	

	public GraphGenerator(final FeatureSource featureSource) {
		this.geofac = new GeometryFactory();
		this.featureSource = featureSource;
	
		log.setLevel(Level.INFO);

	}

	public Collection<Feature> createGraph() throws Exception {
		parseLineStrings();
		 deleteToShortLineStrings();
		cleanUpLineStrings();
		checkForIntersectionsToSplit();
		simplifyNetwork();
		mergeNodes();
		generateNodes();

		return genFeatureCollection();
	}
	
	private void generateNodes() {
		log.info("generating nodes ...");
		int id = 0;
		final ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>(this.lineStrings);
		final HashSet<Point> finished = new HashSet<Point>();
		while (lineStringsQueue.peek() != null) {
			final LineString ls = lineStringsQueue.poll();
			final Collection<Point> po = new ArrayList<Point>(); 
			po.add(ls.getStartPoint());
			po.add(ls.getEndPoint());
			for (final Point p : po) {
				if (finished.contains(p)) {
					continue;
				}
				if (this.nodes.get(p.getX(), p.getY(),GISToMatsimConverter.CATCH_RADIUS).size() > 0) {
					continue;
				}
				
				final Collection<LineString> tmp = this.tree.get(p.getX(), p.getY(), GISToMatsimConverter.CATCH_RADIUS);
				final Node node = new Node();
				node.id = id++;
				node.x = p.getX();
				node.y = p.getY();
				this.nodes.put(node.x, node.y, node);
				for (final LineString l : tmp) {
					if (l.getStartPoint().equalsExact(p,GISToMatsimConverter.CATCH_RADIUS)) {
						finished.add(l.getStartPoint());
					} else if (l.getEndPoint().equalsExact(p,GISToMatsimConverter.CATCH_RADIUS)) {
						finished.add(l.getEndPoint());						
					} else {
					log.error("this should not happen: " + id);
					}
				}
				
			}
			
			
			
		}
		
		log.info("done");
	}

	private void deleteToShortLineStrings(){
//		int rm = 0;
		
		log.info("removing very short LineStrings ...");
		final ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>(this.lineStrings);
		while (lineStringsQueue.peek() != null) {
			final LineString ls = lineStringsQueue.poll();
			if (ls.getLength() >= MIN_LENGTH) {
				continue;
			}
//			System.out.println("removed: " + ++rm);
			remove(ls);
			
			final Point sp = ls.getStartPoint();
			final Point ep = ls.getEndPoint();
			
			final double joinX = (sp.getCoordinate().x + ep.getCoordinate().x) / 2;
			final double joinY = (sp.getCoordinate().y + ep.getCoordinate().y) / 2;
			
			final Collection<LineString> stmp = this.tree.get(sp.getX(), sp.getY(), GISToMatsimConverter.CATCH_RADIUS);
			for (final LineString tmpLs : stmp) {
				if (tmpLs.getStartPoint().distance(sp) < tmpLs.getEndPoint().distance(sp)) {
					tmpLs.getStartPoint().getCoordinate().x = joinX;
					tmpLs.getStartPoint().getCoordinate().y = joinY;
				} else {
					tmpLs.getEndPoint().getCoordinate().x = joinX;
					tmpLs.getEndPoint().getCoordinate().y = joinY;					
				}
				tmpLs.geometryChanged();
				remove(tmpLs);
				add(tmpLs);
			}
			final Collection<LineString> etmp = this.tree.get(ep.getX(), ep.getY(), GISToMatsimConverter.CATCH_RADIUS);
			for (final LineString tmpLs : etmp) {
				if (tmpLs.getStartPoint().distance(ep) < tmpLs.getEndPoint().distance(ep)) {
					tmpLs.getStartPoint().getCoordinate().x = joinX;
					tmpLs.getStartPoint().getCoordinate().y = joinY;
				} else {
					tmpLs.getEndPoint().getCoordinate().x = joinX;
					tmpLs.getEndPoint().getCoordinate().y = joinY;					
				}
				tmpLs.geometryChanged();
				remove(tmpLs);
				add(tmpLs);
			}			

		}
		
		log.info("done.");
		
		
	}
	

	private void cleanUpLineStrings() {
		log.info("removing redundant points ...");
		final ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>(this.lineStrings);
		while (lineStringsQueue.peek() != null) {
			final LineString ls = lineStringsQueue.poll();
			if (ls.getNumPoints() <= 2) {
				log.debug("LineString consists of 2 points only - ignoring!!");
				continue;
			}
			Point p = ls.getStartPoint();
			final Vector<Coordinate> coords = new Vector<Coordinate>();
			coords.add(p.getCoordinate());
			for (int i = 1; i < ls.getNumPoints(); i++) {
				final Point tmp = ls.getPointN(i);
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
				final Coordinate[] carray = new Coordinate[coords.size()];
				for (int i = 0; i < coords.size(); i++) {
					carray[i] = coords.elementAt(i);
				}
				try {
					final LineString cleanLs = this.geofac.createLineString(carray);
					add(cleanLs);
				} catch (final RuntimeException e) {
					e.printStackTrace();
					add(ls);
				}
			}
		}
		log.info("done.");
	}

	private void simplifyNetwork() {

		log.info("unifying fragmented links...");
		final ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>(this.lineStrings);
		while (lineStringsQueue.peek() != null) {
			final LineString ls = lineStringsQueue.poll();
			Collection<LineString> tmp = this.tree.get(ls.getStartPoint().getX(), ls.getStartPoint().getY(), GISToMatsimConverter.CATCH_RADIUS);
			
			
			
			if (tmp.size() != 2) {
				tmp = this.tree.get(ls.getEndPoint().getX(), ls.getEndPoint()
						.getY(), GISToMatsimConverter.CATCH_RADIUS);
			}
			if (tmp.size() == 2) {

				for (final LineString neighbor : tmp) {
					if (neighbor.equals(ls)) {
						continue;
					}
					remove(ls);
					remove(neighbor);
					final LineString union = catAtTouchPoint(ls, neighbor);
					add(union);
//					lineStringsQueue.add(union);

				}

			}

		}
//		for (LineString ls : this.lineStrings) {
//			Collection<LineString> tmp = this.tree.get(ls.getStartPoint().getX(), ls.getStartPoint().getY(), GISToMatsimConverter.CATCH_RADIUS);
//			
//			
//			
//			if (tmp.size() != 2) {
//				tmp = this.tree.get(ls.getEndPoint().getX(), ls.getEndPoint()
//						.getY(), GISToMatsimConverter.CATCH_RADIUS);
//			}
//			if (tmp.size() == 2) {
//				System.err.println("this is not possible!!!");
//				
//			}
//		}
		
		
		log.info("done.");
	}



	private void checkForIntersectionsToSplit() {
		log.info("check if some LineStrings have to be splitted at intersections ...");
		final Iterator<LineString> it = this.lineStrings.iterator();
		final ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()) {
			lineStringsQueue.add(it.next());
		}
		while (lineStringsQueue.peek() != null) {
			final LineString ls = lineStringsQueue.poll();

			final Vector<Point> splitPoints = new Vector<Point>();
			for (int i = 1; i < ls.getNumPoints() - 1; i++) {
				// TODO add public boolean valueExistsAt(x,y,CATCH_RADIUS) to
				// QuadTree
				final Collection<LineString> tmp = this.tree.get(
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
	
		
		final ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>(this.lineStrings);
		final Iterator<LineString> it = lineStringsQueue.iterator();
		while (it.hasNext()) {
			final LineString ls = it.next();
			
			final Point [] points = new Point[]{ls.getStartPoint(), ls.getEndPoint()};
			
			for(int i = 0 ; i < points.length ; i++){
			
				final Point p = points[i];
				final Collection<LineString> lsList = this.tree.get(p.getX(), p.getY(), GISToMatsimConverter.CATCH_RADIUS);
				for (final LineString l : lsList){
					if(l.getStartPoint().equalsExact(p, GISToMatsimConverter.CATCH_RADIUS)){
						if(!l.getStartPoint().equalsExact(p)){
							final Coordinate [] c = l.getCoordinates();
							c[0] = p.getCoordinate(); 
							final CoordinateSequence seq = new CoordinateArraySequence(c);
							final LineString newLine = new LineString(seq, this.geofac);
							remove(l);
							add(newLine);
						}
					} else if(l.getEndPoint().equalsExact(p, GISToMatsimConverter.CATCH_RADIUS)){
						if(!l.getEndPoint().equalsExact(p)){
							final Coordinate [] c = l.getCoordinates();
							c[c.length-1] = p.getCoordinate(); 
							final CoordinateSequence seq = new CoordinateArraySequence(c);
							final LineString newLine = new LineString(seq, this.geofac);
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
		final FeatureCollection collection = this.featureSource.getFeatures();
		final Envelope o = this.featureSource.getBounds();
		this.lineStrings = new HashSet<LineString>();
		this.tree = new QuadTree<LineString>(o.getMinX(), o.getMinY(), o
				.getMaxX(), o.getMaxY());
		this.nodes = new QuadTree<Node>(o.getMinX(), o.getMinY(), o
		.getMaxX(), o.getMaxY());
		final FeatureIterator it = collection.features();
		while (it.hasNext()) {
			final Feature feature = it.next();
			final MultiLineString multiLineString = (MultiLineString) feature
					.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				final LineString lineString = (LineString) multiLineString
						.getGeometryN(i);
				add(lineString);
			}

		}
		log.info("done.");

	}



	private Collection<Feature> genFeatureCollection() throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		final Node dummy = new Node();
		dummy.id = -1;
		final Collection<Feature> features = new ArrayList<Feature>();
		
		final AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, this.featureSource.getSchema().getDefaultGeometry().getCoordinateSystem());
		final AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", Integer.class);
		final AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", Integer.class);
		final AttributeType toNode = AttributeTypeFactory.newAttributeType(
				"toID", Integer.class);		
		final FeatureType ftRoad = FeatureTypeFactory.newFeatureType(
				new AttributeType[] { geom, id, fromNode, toNode }, "link");
		int ID = 0;
		for (final LineString ls : this.lineStrings){
			
			Collection<Node> nodes = this.nodes.get(ls.getStartPoint().getX(), ls.getStartPoint().getY(), 0.1);
			Node from = null;
			if (nodes.size() != 1) {
				from = dummy;
			} else {
				from = nodes.iterator().next();
			}

			nodes = this.nodes.get(ls.getEndPoint().getX(), ls.getEndPoint().getY(), 0.1);
			Node to = null;
			if (nodes.size() != 1) {
				to = dummy;
			} else {
				to = nodes.iterator().next();
			}
			
			final Feature ft = ftRoad.create(new Object [] {new MultiLineString(new LineString []{ls},this.geofac) , ID++, from.id,to.id},"network");
			features.add(ft);
				
		}


		return features;
	}

	private void add(final LineString ls) {
		this.lineStrings.add(ls);
		this.tree.put(ls.getStartPoint().getX(), ls.getStartPoint().getY(), ls);
		this.tree.put(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
	}

	private void remove(final LineString ls) {
		this.lineStrings.remove(ls);
		this.tree.remove(ls.getStartPoint().getX(), ls.getStartPoint().getY(),
				ls);
		this.tree.remove(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
	}

	
	private LineString catAtTouchPoint(final LineString ls1, final LineString ls2) {

		final Coordinate[] carray = new Coordinate[ls1.getNumPoints() + ls2.getNumPoints() - 1];
		boolean reverseLs = false;
		boolean reverseNeighbor = false;
		if (ls1.getStartPoint().equalsExact(ls2.getStartPoint(), GISToMatsimConverter.CATCH_RADIUS) || ls1.getStartPoint().equalsExact(ls2.getEndPoint(),	GISToMatsimConverter.CATCH_RADIUS)) {
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

		final int offset = ls1.getNumPoints() - 1;
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

	private void splitLineString(final LineString ls, final Vector<Point> splitPoints) {
		
		splitPoints.add(ls.getEndPoint());
		Point tmp = ls.getStartPoint();
		final Vector<Coordinate> segment = new Vector<Coordinate>();
		int count = 0;
		for (final Point splitPoint : splitPoints) {
			while (!tmp.equals(splitPoint)) {
				segment.add(tmp.getCoordinate());
				tmp = ls.getPointN(count++);
			}
			segment.add(tmp.getCoordinate());
			final Coordinate[] coords = new Coordinate[segment.size()];
			for (int j = 0; j < segment.size(); j++) {
				coords[j] = segment.elementAt(j);
			}
			final LineString subLs = this.geofac.createLineString(coords);
			add(subLs);
			segment.clear();
		}

	}
	
	private static class Node {
		int id;
		double x;
		double y;
		
	}		
		
		
		
		
		
		
		
		
}
