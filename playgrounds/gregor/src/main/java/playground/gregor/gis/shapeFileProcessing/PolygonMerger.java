/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonMerger.java
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

package playground.gregor.gis.shapeFileProcessing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PolygonMerger {
	
	private static final Logger log = Logger.getLogger(PolygonMerger.class);
	
	private PolygonGeneratorII pg;
	private boolean readFromFile = false;
	private String file;
	private HashMap<Integer, Polygon> merged;
	private int optid = 0;

	private boolean saveToFile = false;
	
	
	
	public PolygonMerger(PolygonGeneratorII pg) {
		this.pg = pg;
	}
	public PolygonMerger(PolygonGeneratorII pg, String file){
		this.pg = pg;
		this.saveToFile  = true;
		this.file = file;
	}
	
	public PolygonMerger(String file){
		this.readFromFile = true;
		this.file = file;
	}
	
	
	public HashMap<Integer, Polygon> getMergedPolygons() throws Exception {
		
		if (this.readFromFile) {
			readFromFile();
		} else {
			merge();
		}
		if (this.saveToFile) {
			ShapeFileWriter.writeGeometries(getPolygonFeatures(), this.file);

		}
		
		
		return this.merged;
		
	}
	
	private Collection<Feature> getPolygonFeatures() {
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		for (Iterator<Entry<Integer, Polygon>> it = this.merged.entrySet().iterator() ; it.hasNext() ; ){
			Entry<Integer, Polygon> e = it.next();
			features.add(this.pg.getPolygonFeature(e.getValue(), 0, e.getKey(),0,0,0,0)); 
				
		}
		return features;
	}
	private void readFromFile() throws Exception {
		this.merged = new HashMap<Integer,Polygon>();
		FeatureSource ft = ShapeFileReader.readDataFile(this.file);
		FeatureIterator it = ft.getFeatures().features();
		while (it.hasNext()) {
			Feature feature = it.next();
			int id = (Integer) feature.getAttribute(1);
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
				continue;
			}
			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			this.merged.put(id, polygon);

		}
		
	}

	private void merge(){
		
		log.info("merging polygons ...");
		
		HashMap<Integer, Polygon> returnPolys = new HashMap<Integer, Polygon>();
		
		for (Iterator<Integer> lsIt = this.pg.getLineStringsMap().keySet().iterator() ; lsIt.hasNext() ; ){
		
			Integer id = (Integer) lsIt.next();
						
			LineString ls = this.pg.getLineStringsMap().get(id);
			
			HashSet<Polygon> neighborhood = new HashSet<Polygon>();
			Collection<Polygon> polys = this.pg.getPolygonQuadTree().get(ls.getCentroid().getX(),ls.getCentroid().getY() ,2000);

			
			for (Polygon po : polys){
				if(ls.intersects(po)) { 
					neighborhood.add(po);
				}	
			}
			List<Polygon> extNeighborhood = new ArrayList<Polygon>();
    		extNeighborhood.addAll(neighborhood);
    		for (Polygon po : polys) {
    			if (!neighborhood.contains(po)){
    				for (Polygon tmp : neighborhood) {
    					
//    					if (po.intersects(tmp)||po.touches(tmp) || ls.getStartPoint().distance(po) < CATCH_RADIUS || ls.getEndPoint().distance(po) < CATCH_RADIUS ){
    					if (po.intersects(tmp) ){
    						extNeighborhood.add(po);
    						break;
    					} 
    				}
    			}
    		}
			
			if(extNeighborhood.isEmpty()){	continue;}

			Geometry [] gA = new Geometry[0];
   			Geometry [] geoArray = extNeighborhood.toArray(gA);
   			GeometryCollection geoColl = new GeometryCollection(geoArray,this.pg.getGeofac());	
   			   			 			
   			try{
   				Geometry retPoly = null;
   				for (double dbl = 0.05; dbl <= 0.551; dbl += 0.05) {
   					retPoly = (Geometry) geoColl.buffer(dbl);
	   				if (retPoly.getNumGeometries() > 1) {
//	   					if (dbl >= 0.5) {
//	   						log.warn("Multipolygon produced in mergePolygons() - increasing radius!");
////	   			   			for (int i = 0; i < retPoly.getNumGeometries(); i++) {
////	   							Polygon polygon = (Polygon) retPoly.getGeometryN(i);
////	   							this.pg.createPolygonFeature(polygon,3,id,0,0,0,0);
////	   			   			}	   						
//	   					} else {
//	   						log.info("Multipolygon produced in mergePolygons() - increasing radius!");
//	   					}
	   				} else {
	   					break;
	   				}
	   				
   				}
   				
   				
	   			for (int i = 0; i < retPoly.getNumGeometries(); i++) {
					Polygon polygon = (Polygon) retPoly.getGeometryN(i);
					
					if(!polygon.isEmpty()){	
						polygon = setAdditionalIntersects(polygon,ls.getStartPoint());
						polygon = setAdditionalIntersects(polygon,ls.getEndPoint());
						returnPolys.put( id ,polygon);
					}
				}
   			}catch(Exception e){
   				
//   				this.pg.createLineStringFeature(ls,this.optid--, -1 + "");
//   				for (Polygon tmp : extNeighborhood) {
//   					this.pg.createPolygonFeature(tmp, this.optid, id,0,0,0,0);	
//   				}
   				
   				e.printStackTrace();
   			}
   		}
		this.merged = returnPolys;
		log.info("done.");
		
		
	}
	
	private Polygon setAdditionalIntersects(Polygon poly, Point currPoint) {
		Collection<LineString> ls = this.pg.getLineStringQuadTree().get(currPoint.getX(), currPoint.getY(),PolygonGeneratorII.CATCH_RADIUS);
		
		for (LineString l : ls) {
			Point p = null;			
			if (l.getStartPoint().equalsExact(currPoint, PolygonGeneratorII.CATCH_RADIUS)) {
				p = l.getPointN(1);
			} else if (l.getEndPoint().equalsExact(currPoint, PolygonGeneratorII.CATCH_RADIUS)) {
				p = l.getPointN(l.getNumPoints()-2);
			} else {
				throw new RuntimeException("this should not happen!!!");
			}
			double x_diff = currPoint.getX() - p.getX();
			double y_diff = currPoint.getY() - p.getY();
			double length = Math.max(Math.sqrt(x_diff*x_diff + y_diff * y_diff),20.0);
			double scale =  l.getLength()/length ;
			x_diff *= scale;
			y_diff *= scale;
			Coordinate [] c = new Coordinate [] {new Coordinate(currPoint.getX() - x_diff, currPoint.getY() - y_diff), new Coordinate(currPoint.getX() + x_diff, currPoint.getY() + y_diff)};
			LineString tmp = this.pg.getGeofac().createLineString(c);

			List<LineString> rings = new ArrayList<LineString>();
			rings.add(poly.getExteriorRing());
						
			for (int i = 0; i < poly.getNumInteriorRing(); i++){
				rings.add(poly.getInteriorRingN(i));
			}
			
			LineString [] lineStrings = new LineString[rings.size()];
			rings.toArray(lineStrings);
						
			MultiLineString mls = this.pg.getGeofac().createMultiLineString(lineStrings);
			Geometry v =  mls.intersection(tmp);
	
//		
//			int idx = 0;
//			double dist = Double.MAX_VALUE;
//			for (int i = 0; i < v.getNumGeometries(); i++) {
//				double tmpdist = ((Point)v.getGeometryN(i)).distance(currPoint);
//				if ( tmpdist < dist){
//					dist = tmpdist;
//					idx = i;
//				}
//				
//			}
			
			
			if (!v.isEmpty()) {
				for (int idx = 0; idx < v.getNumGeometries(); idx++) {
					poly = this.pg.addVertex(poly,(Point) v.getGeometryN(idx));
				}
			}
			
		}
		return poly;
		
	}


}
