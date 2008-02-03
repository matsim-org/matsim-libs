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
import java.util.PriorityQueue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.matsim.controler.Controler;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.collections.QuadTree;



import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author laemmel
 *
 */
public class NetworkBuilder {

	private static final Logger log = Logger.getLogger(NetworkBuilder.class);

	private static final double CATCH_RADIUS = 0.5;

	private NetworkLayer network = null;
	private final FeatureSource featureSource;

	private HashSet<LineString> lineStrings;
	private QuadTree tree;

	private GeometryFactory geofac;

	//DEBUG
	ShapefileDataStore ds = null;

	public NetworkBuilder(FeatureSource featureSource){
		this.geofac = new GeometryFactory();
		this.network = new NetworkLayer();
		this.featureSource = featureSource;


	}

	public  NetworkLayer createNetwork() throws Exception {
		parseLineStrings();
		cleanUpLineStrings();
		splitAtIntersections();
		simplifyNetwork();


		writeGeometries();
		return null;
	}

	private void cleanUpLineStrings() {
		Iterator<LineString> it =  this.lineStrings.iterator();
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()){
			lineStringsQueue.add((LineString) it.next());
		}
		while (lineStringsQueue.peek() != null) {
			LineString ls = lineStringsQueue.poll();
			Point p = ls.getStartPoint();
			Vector<Coordinate> coords = new Vector<Coordinate>();
			coords.add(p.getCoordinate());
			for (int i = 1; i < ls.getNumPoints(); i++){
				Point tmp = ls.getPointN(i);
				if (p.distance(ls.getPointN(i)) <= CATCH_RADIUS ) {
					if (tmp.equals(ls.getEndPoint())){
						coords.remove(coords.lastElement());
						coords.add(tmp.getCoordinate());
					}
				} else {
					coords.add(tmp.getCoordinate());
				}
				p = tmp;
			}
			if (coords.size() < ls.getNumPoints()){
				this.lineStrings.remove(ls);
				this.tree.remove(ls.getStartPoint().getX(), ls.getStartPoint().getY(), ls);
				this.tree.remove(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
				Coordinate [] carray = new Coordinate [coords.size()];
				for (int i = 0; i < coords.size(); i++){
					carray[i] = coords.elementAt(i);
				}
				LineString cleanLs = this.geofac.createLineString(carray);
				this.lineStrings.add(cleanLs);
				this.tree.put(cleanLs.getStartPoint().getX(), cleanLs.getStartPoint().getY(), cleanLs);
				this.tree.put(cleanLs.getEndPoint().getX(), cleanLs.getEndPoint().getY(), cleanLs);
				
			}
		}
	}

	private void simplifyNetwork() {
		Iterator<LineString> it =  this.lineStrings.iterator();
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()){
			lineStringsQueue.add((LineString) it.next());
		}
		while (lineStringsQueue.peek() != null) {
			LineString ls = lineStringsQueue.poll();
			Collection<LineString> tmp = this.tree.get(ls.getStartPoint().getX(), ls.getStartPoint().getY(), CATCH_RADIUS);
			if (tmp.size() != 2){
				tmp = this.tree.get(ls.getEndPoint().getX(), ls.getEndPoint().getY(), CATCH_RADIUS);
			}
			if (tmp.size() == 2) {
				int ii=0; ii++;
				for (LineString neighbor : tmp){

					if (neighbor.equals(ls)) {
						continue;
					}
					

					
					this.lineStrings.remove(ls);
					this.tree.remove(ls.getStartPoint().getX(), ls.getStartPoint().getY(), ls);
					this.tree.remove(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
					this.lineStrings.remove(neighbor);
					this.tree.remove(neighbor.getStartPoint().getX(), neighbor.getStartPoint().getY(), neighbor);
					this.tree.remove(neighbor.getEndPoint().getX(), neighbor.getEndPoint().getY(), neighbor);
					

					Coordinate [] carray = new Coordinate [ls.getNumPoints() + neighbor.getNumPoints() -1];

					boolean reverseLs = false;
					boolean reverseNeighbor = false;
					if (ls.getStartPoint().equalsExact(neighbor.getStartPoint(), CATCH_RADIUS) || ls.getStartPoint().equalsExact(neighbor.getEndPoint(), CATCH_RADIUS)){
						reverseLs = true;
						if (ls.getStartPoint().equalsExact(neighbor.getEndPoint(), CATCH_RADIUS)){
							reverseNeighbor = true;
						}
					} else {
						if (ls.getEndPoint().equalsExact(neighbor.getEndPoint(), CATCH_RADIUS)){
							reverseNeighbor = true;
						}	
					}
					
					
					
					if (reverseLs) {
						int j = 0;
						for (int i = ls.getNumPoints()-1; i >= 0; i --){
							carray[j++] = ls.getCoordinateN(i);
						}
					} else {
						for (int i = 0; i < ls.getNumPoints(); i ++){
							carray[i] = ls.getCoordinateN(i);
						}
					}
						
					int offset = ls.getNumPoints() - 1;
					if (reverseNeighbor) {
						int j = offset + 1;
						for (int i = neighbor.getNumPoints() -2 ; i >= 0; i--){
							carray[j++] = neighbor.getCoordinateN(i);
						} 
					} else {
						for (int i = 1; i< neighbor.getNumPoints(); i++){
							carray[i+offset] = neighbor.getCoordinateN(i);
						}						
					}

					LineString union = this.geofac.createLineString(carray);				
					this.lineStrings.add(union);
					this.tree.put(union.getStartPoint().getX(), union.getStartPoint().getY(), union);
					this.tree.put(union.getEndPoint().getX(), union.getEndPoint().getY(), union);					
					lineStringsQueue.add(union);
					break;
				}
			}
		}

	}

	private void splitAtIntersections() {
		log.info("check if some LineStrings have to be splitted at intersections ...");
		Iterator<LineString> it =  this.lineStrings.iterator();
		ConcurrentLinkedQueue<LineString> lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()){
			lineStringsQueue.add((LineString) it.next());
		}
		while (lineStringsQueue.peek() != null) {
			LineString ls = lineStringsQueue.poll();

			Vector<Point> splitPoints = new Vector<Point>();
			for (int i = 1; i < ls.getNumPoints()-1; i++){
				//TODO add  public boolean valueExistsAt(x,y,CATCH_RADIUS) to QuadTree
				Collection<LineString> tmp = this.tree.get(ls.getCoordinateN(i).x, ls.getCoordinateN(i).y, CATCH_RADIUS);
				if (!tmp.isEmpty()){
					splitPoints.add(ls.getPointN(i));
				}
			}
			if (splitPoints.size() > 0) {
				splitLineString(ls,splitPoints);
			}
		}
		log.info("done.");

	}

	private void splitLineString(LineString ls, Vector<Point> splitPoints) {
		this.lineStrings.remove(ls);
		this.tree.remove(ls.getStartPoint().getX(), ls.getStartPoint().getY(), ls);
		this.tree.remove(ls.getEndPoint().getX(), ls.getEndPoint().getY(), ls);
		splitPoints.add(ls.getEndPoint());
		Point tmp = ls.getStartPoint();
		Vector<Coordinate> segment = new Vector<Coordinate>();
		int count = 0;
		for (Point splitPoint : splitPoints){
			while (!tmp.equals(splitPoint)){
				segment.add(tmp.getCoordinate());
				tmp = ls.getPointN(count++);
			}
			segment.add(tmp.getCoordinate());
			Coordinate [] coords = new Coordinate[segment.size()]; 
			for (int j=0; j < segment.size(); j++ ) {
				coords[j] = segment.elementAt(j);
			}			
			LineString subLs = this.geofac.createLineString(coords);
			this.lineStrings.add(subLs);
			this.tree.put(subLs.getStartPoint().getX(), subLs.getStartPoint().getY(), subLs);
			this.tree.put(subLs.getEndPoint().getX(), subLs.getEndPoint().getY(), subLs);
			segment.clear();
		}


	}


	private void parseLineStrings() throws IOException {


		log.info("parsing features and building up QuadTree ...");
		FeatureCollection collection = this.featureSource.getFeatures();
		Envelope o = this.featureSource.getBounds();
		this.lineStrings = new HashSet<LineString>();
		this.tree = new QuadTree(o.getMinX(),o.getMinY(),o.getMaxX(),o.getMaxY());

		FeatureIterator it = collection.features();
		while (it.hasNext()){
			Feature feature = it.next();
			Integer ii = (Integer) feature.getAttribute(1);
			MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				LineString lineString = (LineString) multiLineString.getGeometryN(i);
				lineString.setSRID(ii);
				Coordinate from = lineString.getStartPoint().getCoordinate();
				this.tree.put(from.x, from.y, lineString);
				Coordinate to = lineString.getEndPoint().getCoordinate();
				this.tree.put(to.x, to.y, lineString);

				this.lineStrings.add(lineString);
			}

		}
		log.info("done.");

	}


	//DEBUG
	private void writeGeometries() throws Exception {
		File inFile = new File("./padang/debug.shp");
		ds = new ShapefileDataStore(inFile.toURL());
		FeatureType ft = this.featureSource.getSchema();
		File out = new File("./padang/debug_out.shp");

		String name = ds.getTypeNames()[0];

		ShapefileDataStore outStore = new ShapefileDataStore(out.toURL());
		outStore.createSchema(this.featureSource.getSchema());
		FeatureSource newFeatureSource = outStore.getFeatureSource(name);
		FeatureStore newFeatureStore = (FeatureStore)newFeatureSource;

		// accquire a transaction to create the shapefile from FeatureStore
		Transaction t = newFeatureStore.getTransaction();

		FeatureCollection collect = new PFeatureCollection(ft);

		Iterator it = this.lineStrings.iterator();
		while (it.hasNext()){
			LineString ls = (LineString) it.next();
			MultiLineString mls =  new MultiLineString(new LineString[]{ls},geofac);
			Feature feature = ft.create(null);
			feature.setDefaultGeometry(mls);
			collect.add(feature);

		}
		newFeatureStore.addFeatures(collect);

		t.commit();
		t.close();


	}
	public static class PFeatureCollection extends DefaultFeatureCollection {
		public PFeatureCollection(FeatureType ft) {
			super("error",ft );
		}
	}
}
