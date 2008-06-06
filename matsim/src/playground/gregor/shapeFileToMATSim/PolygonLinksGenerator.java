/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonLinksGenerator.java
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

package playground.gregor.shapeFileToMATSim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.geotools.feature.SchemaException;
import org.matsim.utils.collections.QuadTree;
import org.opengis.referencing.FactoryException;

import com.sun.opengl.impl.Java2D;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PolygonLinksGenerator {

	private static final Logger log = Logger.getLogger(PolygonLinksGenerator.class);
	
	private PolygonGeneratorII pg;
	private String file;
	private boolean saveToFile = false;
	private Map<Integer, Feature> ftLinks;

	private int id;
	
	
	public PolygonLinksGenerator(PolygonGeneratorII pg) {
		this.pg = pg;
	}
	public PolygonLinksGenerator(PolygonGeneratorII pg, String file){
		this.pg = pg;
		this.saveToFile  = true;
		this.file = file;
	}
	
	public void getPolygonLinks(QuadTree<Feature> nodeFeatures, HashMap<Integer,Polygon> merged) throws Exception, FactoryException, SchemaException{
		
		cutPolygonLinks(nodeFeatures, merged);
		calculateMinWidth();
		
		if (this.saveToFile) {
			ShapeFileWriter.writeGeometries(this.ftLinks.values(), this.file);
		}
		
	}
	
	private void calculateMinWidth() {
		for (Feature link : this.ftLinks.values()) {
			MultiPolygon mp = (MultiPolygon) link.getDefaultGeometry(); 
			Polygon p = (Polygon) mp.getGeometryN(0);
			int id = (Integer) link.getAttribute(1);
			if (this.pg.getLineStringsMap().get(id).getLength() < 50) {
				continue;
			}
			
			double minWidth = getMinWidth(p,this.pg.getLineStringsMap().get(id));
			break;
		}
		
	}
	public void cutPolygonLinks(QuadTree<Feature> nodeFeatures, HashMap<Integer,Polygon> merged) {
		//TODO polygons zurechtschneiden - minmale breite ermittlen - flaeche ermitteln und in eine  Datei schreiben ...
		
//		Map<Integer,Polygon> pLinks = new HashMap<Integer,Polygon>(this.pg.getLineStringsMap().entrySet().size());
		this.ftLinks = new HashMap<Integer,Feature>(this.pg.getLineStringsMap().entrySet().size());
		
	 	for(Iterator<Entry<Integer, LineString>> lsIter = this.pg.getLineStringsMap().entrySet().iterator() ; lsIter.hasNext() ; ){
			
	 		
			Entry<Integer, LineString> lsEntry  = lsIter.next();
			int id = lsEntry.getKey();
			LineString currLs = lsEntry.getValue();
			List<Point> po = new ArrayList<Point>();
			po.add(currLs.getStartPoint());
			po.add(currLs.getEndPoint());
			Polygon pLink = merged.get(id);
			if (pLink == null) {
				log.warn("no merged polygon found for LineString " + id + "!");
				this.pg.createLineStringFeature(currLs, id, Integer.toString(id));
				continue;
			}
			
			List<Integer> nodeIds  = new ArrayList<Integer>(2);
			
			for (Point currPoint : po) {
				Feature ftNode = nodeFeatures.get(currPoint.getX(), currPoint.getY());
				if (ftNode == null) {
					log.warn("no polygon node found for LineString" + id + "!"); 
					this.pg.createLineStringFeature(currLs, id, "");
					break;
				}
				nodeIds.add((Integer) ftNode.getAttribute(1));
				
				
				MultiPolygon mpNode = (MultiPolygon) ftNode.getDefaultGeometry();
				if (mpNode.getNumGeometries() > 1) {
					log.warn("MultiPolygons with more then 1 Geometry ignored!");
					this.pg.createLineStringFeature(currLs, id, "");
					continue;
				}
				Polygon pNode = (Polygon) mpNode.getGeometryN(0);
				
				Collection<Polygon> separated =  separatePoly(pLink, pNode);
				int found = 0;
				for (Polygon p : separated) {
					if(currLs.crosses(p) || p.contains(currLs) ) {
//						//DEBUG
//						if (++found > 1) {
//							log.warn("found more then 1 matching polygon, possible something is  wrong with pNode ");
//							this.pg.createPolygonFeature(p, found, id, 0, 0, 0, 0);
//						}
						
						pLink = p;
					}
				}
				
				
				
				
				if (found >1) {
//					//DEBUG
//					this.pg.createPolygonFeature(pNode, found, id, 0, 0, 0, 0);
					pLink = null;
					break;
				}
				
			}

			if (pLink == null) {
				log.warn("could not create link polygon for LineString:" + id + "!");
				this.pg.createLineStringFeature(currLs, id, "");
				continue;
			}
			
			int i = 0;
			for(Iterator<Entry<Integer, LineString>> lsIt = this.pg.getLineStringsMap().entrySet().iterator() ; lsIt.hasNext() ; ){
				LineString lsEn = lsIt.next().getValue();
				if (lsEn.intersects(pLink) || lsEn.touches(pLink)) i++;
				if (i > 1) {
//					log.warn(" ");
					break;
				}
			}
			if(i > 1) {
				this.pg.createLineStringFeature(currLs, id, "");
				continue;
			}
			
//			double minWidth = getMinWidth(pLink,currLs);
			Feature ftLink = this.pg.getPolygonFeature(pLink, 0, id, nodeIds.get(0), nodeIds.get(1), 0, 0);
			this.ftLinks.put(id, ftLink);
	 	}
		
		
	}
		
	private double getMinWidth(Polygon p, LineString l) {
		Coordinate [] coords = p.getCoordinates();
		double minWidth = Double.POSITIVE_INFINITY;
		for (int i = 0; i < coords.length-1; i++) {
			double t = getWidth2(coords[i],p,l);
			if (t < minWidth) {
				minWidth = t;
			}
			
		}
		
		
		
		return minWidth;
	}
	private double getWidth2(Coordinate c, Polygon p, LineString l) {

		Coordinate perp = getPerpendicularCoordinate(c,l);
		double dx = c.x - perp.x;
		double dy = c.y - perp.y;
		double distPerp = c.distance(perp);
		double scale = (l.getLength()/2)/distPerp;
		Coordinate c1 = new Coordinate(c.x + dx*scale, c.y + dy*scale);
		Coordinate c2 = new Coordinate(c.x - dx*scale, c.y - dy*scale);
		
		Coordinate [] cc1 = new Coordinate [] {c,c1};
		LineString lcc1 = this.pg.getGeofac().createLineString(cc1);
		Coordinate [] cc2 = new Coordinate [] {c,c2};
		LineString lcc2 = this.pg.getGeofac().createLineString(cc2);
					
		
		
//		this.pg.createLineStringFeature(lc1c2,0, "");
		
		Geometry intP = null;
		Geometry intL = null;
		try {
			intP = p.intersection(lcc1);
			intL = l.intersection(lcc1);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		if (intP.getNumGeometries() != 2){
			try {
				intP = p.intersection(lcc2);
				intL = l.intersection(lcc2);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}				
		}
		
		
		if (intL.getNumGeometries() == 1 && intP.getNumGeometries() == 2) {
//			double dc1l = intP.getGeometryN(0).distance(intL.getGeometryN(0));
//			double dc1c2 = intP.getGeometryN(0).distance(intP.getGeometryN(1));
//			
			
			
			Geometry g1 = intP.getGeometryN(0);
			Geometry g2 = intP.getGeometryN(1);
			Coordinate [] wl = new Coordinate[] {g1.getCoordinate(), g2.getCoordinate()};
			LineString lwidth = this.pg.getGeofac().createLineString(wl);
			if (!p.covers(lwidth)) {
				return Double.POSITIVE_INFINITY;
			}
			
			
			double width = g1.distance(g2);
			
			this.pg.createLineStringFeature(this.pg.getGeofac().createLineString(wl),1, width +"");
			return width;			

		}

		return Double.POSITIVE_INFINITY;
		
		
	}
	
	
	private Coordinate getPerpendicularCoordinate(Coordinate c, LineString l) {
		LineSegment ls = getLineSegment(c, l);
		Coordinate a = ls.p0;
		Coordinate b = ls.p1;
		
		double r = (a.y-c.y) * (a.y - b.y) -(a.x-c.x) * (b.x-a.x) / Math.pow(ls.getLength(),2);
		Coordinate p = new Coordinate(a.x+r*(b.x-a.x), a.y+r*(b.y-a.y));
		return p;
	}
	private LineSegment getLineSegment(Coordinate c, LineString l) {
		LineSegment ls = null;
		double minDist = Double.POSITIVE_INFINITY;
		for (int i =1; i < l.getNumGeometries(); i++){
			Coordinate c1 = l.getGeometryN(i-1).getCoordinate();
			Coordinate c2 = l.getGeometryN(i).getCoordinate();
			LineSegment temp = new LineSegment(c1,c2);
			double td = temp.distance(c);
			if (td < minDist) {
				minDist = td;
				ls = temp;
			}
			
		}
		return ls;
	}
	private double getWidth(Coordinate c, Polygon p, LineString l) {
		double minWidth = Double.POSITIVE_INFINITY;
		for (double alpha = 0; alpha < 2*Math.PI; alpha += Math.PI/18) {
			double dx = Math.cos(alpha);
			double dy = Math.sin(alpha);
			
			
			
			Coordinate  c1 = new Coordinate(c.x + l.getLength() * dx, c.y + l.getLength() * dy);
			Coordinate  c2 = new Coordinate(c.x - l.getLength() * dx, c.y - l.getLength() * dy);
			
			Coordinate [] cc1 = new Coordinate [] {c,c1};
			LineString lcc1 = this.pg.getGeofac().createLineString(cc1);
			Coordinate [] cc2 = new Coordinate [] {c,c2};
			LineString lcc2 = this.pg.getGeofac().createLineString(cc2);
						
			
			
//			this.pg.createLineStringFeature(lc1c2,0, "");
			
			Geometry intP = null;
			Geometry intL = null;
			try {
				intP = p.intersection(lcc1);
				intL = l.intersection(lcc1);
			} catch (RuntimeException e) {
				e.printStackTrace();
				continue;
			}
			if (intP.getNumGeometries() != 2){
				try {
					intP = p.intersection(lcc2);
					intL = l.intersection(lcc2);
				} catch (RuntimeException e) {
					e.printStackTrace();
					continue;
				}				
			}
			
			
			if (intL.getNumGeometries() == 1 && intP.getNumGeometries() == 2) {
//				double dc1l = intP.getGeometryN(0).distance(intL.getGeometryN(0));
//				double dc1c2 = intP.getGeometryN(0).distance(intP.getGeometryN(1));
//				
				
				
				Geometry g1 = intP.getGeometryN(0);
				Geometry g2 = intP.getGeometryN(1);
				Coordinate [] wl = new Coordinate[] {g1.getCoordinate(), g2.getCoordinate()};
				LineString lwidth = this.pg.getGeofac().createLineString(wl);
				if (!p.covers(lwidth)) {
					continue;
				}
				
				
				double width = g1.distance(g2);
				
				this.pg.createLineStringFeature(this.pg.getGeofac().createLineString(wl),1, width +"");
				
				if (width < minWidth) {
					minWidth = width;
					this.pg.createLineStringFeature(this.pg.getGeofac().createLineString(wl),2, width +"");
				}				
			}

			
		}
		
		return minWidth;
	}
	private Collection<Polygon> separatePoly(Polygon pLink , Polygon pNode){
		
			Collection<Polygon> p = new ArrayList<Polygon>();
			try {
				Polygon b = (Polygon) pNode.buffer(0.5);
				Geometry geo = (Geometry) pLink.difference(b);
				for (int i = 0; i<geo.getNumGeometries(); i++) {
					p.add((Polygon) geo.getGeometryN(i));
				}
			} catch (RuntimeException e) {
				this.id--;
				this.pg.createPolygonFeature(pLink, 0, this.id,0, 0,0, 0);
				this.pg.createPolygonFeature(pNode, 0, this.id,1, 0,0, 0);
				
				e.printStackTrace();
			}
			return p;		
	}
	
	


}
