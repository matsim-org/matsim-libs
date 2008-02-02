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
	ConcurrentLinkedQueue<LineString> lineStringsQueue;
	private QuadTree tree;
	
	private GeometryFactory geofac;
	
	//DEBUG
	ShapefileDataStore ds = null;
	
	public NetworkBuilder(FeatureSource featureSource){
		PrecisionModel pm = new PrecisionModel(10);
		this.geofac = new GeometryFactory();
		this.network = new NetworkLayer();
		this.featureSource = featureSource;
		
		
	}
	
	public  NetworkLayer createNetwork() throws Exception {
		parseFeatures();
		equalizeFeatures();
		writeGeometries();
		return null;
	}

	private void equalizeFeatures() {
		Iterator it =  this.lineStrings.iterator();
		 this.lineStringsQueue = new ConcurrentLinkedQueue<LineString>();
		while (it.hasNext()){
			this.lineStringsQueue.add((LineString) it.next());
		}
		
		while (this.lineStringsQueue.peek() != null) {
			int size = this.lineStringsQueue.size();
//			ArrayList<LineStringStruct> tempRemoved = new ArrayList<LineStringStruct>();
			LineString ls = this.lineStringsQueue.poll();
			if (!this.lineStrings.contains(ls) || ls.isEmpty()){
				continue;
			}
			double startX = ls.getStartPoint().getX();
			double startY = ls.getStartPoint().getY();
			double endX = ls.getEndPoint().getX();
			double endY = ls.getEndPoint().getY();
			
//			LineString neighbor = this.tree.get(ls.getStartPoint().getX(), y, distance)
			Collection<LineString> neighbors = this.tree.get(startX, startY, CATCH_RADIUS);
			for (LineString neighbor : neighbors){
				if (!this.lineStrings.contains(neighbor)) {
					continue;
				}
				if (needToSplit(neighbor,ls.getStartPoint())) {
					split(neighbor,ls.getStartPoint());				
				}
			}
			neighbors = this.tree.get(endX, endY, CATCH_RADIUS);
			for (LineString neighbor : neighbors){
				if (!this.lineStrings.contains(neighbor)) {
					continue;
				}
				if (needToSplit(neighbor,ls.getEndPoint())) {
					split(neighbor,ls.getEndPoint());				
				}
			}
   
		}
		
	}

	private void split(LineString ls, Point splitPoint){
		
		this.lineStrings.remove(ls);
		Vector<Coordinate> seg1 = new Vector<Coordinate>();
		Vector<Coordinate> seg2 = new Vector<Coordinate>();
		boolean isFirstSeg = true;
		for (int i = 0; i < ls.getNumPoints(); i++){
			Point p = ls.getPointN(i);
			if (isFirstSeg) {
				seg1.add(p.getCoordinate());
				if (splitPoint.equalsExact(p, CATCH_RADIUS)){
					isFirstSeg = false;
				}
			}
			if (!isFirstSeg){
				seg2.add(p.getCoordinate());
			}
		}
		Coordinate [] coords1 = new Coordinate[seg1.size()]; 
		for (int m=0; m < seg1.size(); m++ ) {
			coords1[m] = seg1.elementAt(m);
		}
		Coordinate [] coords2 = new Coordinate[seg2.size()]; 
		for (int m=0; m < seg2.size(); m++ ) {
			coords2[m] = seg2.elementAt(m);
		}
		
		LineString ls1 = this.geofac.createLineString(coords1);
		LineString ls2 = this.geofac.createLineString(coords2);
		this.lineStrings.add(ls1);
		this.lineStrings.add(ls2);
		this.lineStringsQueue.add(ls1);
		this.lineStringsQueue.add(ls2);
		for (int j = 0; j < ls1.getNumPoints(); j++){
			this.tree.put(ls1.getCoordinateN(j).x, ls1.getCoordinateN(j).y, ls1);
		}
		for (int j = 0; j < ls2.getNumPoints(); j++){
			this.tree.put(ls2.getCoordinateN(j).x, ls2.getCoordinateN(j).y, ls2);
		}
	}
	
	private boolean needToSplit(LineString neighbor, Point p) {
		if (p.equalsExact(neighbor.getStartPoint(), CATCH_RADIUS)){
			return false;
		}
		if (p.equalsExact(neighbor.getEndPoint(), CATCH_RADIUS)){
			return false;
		}		
		return true;
	}


	private void parseFeatures() throws IOException {
		
		log.info("parsing features and building up QuadTree ...");
		 FeatureCollection collection = this.featureSource.getFeatures();
		 Envelope o = this.featureSource.getBounds();
		 this.lineStrings = new HashSet<LineString>();
		 this.tree = new QuadTree(o.getMinX(),o.getMinY(),o.getMaxX(),o.getMaxY());
		 
		 FeatureIterator it = collection.features();
		while (it.hasNext()){
			Feature feature = it.next();
			MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				LineString lineString = (LineString) multiLineString.getGeometryN(i);
				
				for (int j = 0; j < lineString.getNumPoints(); j++){
					double x = lineString.getCoordinateN(j).x;
					double y = lineString.getCoordinateN(j).y;
					tree.put(x, y, lineString);
				}
				
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
