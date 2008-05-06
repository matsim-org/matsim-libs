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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
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
		
		if (this.saveToFile) {
			ShapeFileWriter.writeGeometries(this.ftLinks.values(), this.file);
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
				log.warn("no merged polygon found for LineString" + id + "!");
				continue;
			}
			
			List<Integer> nodeIds  = new ArrayList<Integer>(2);
			
			for (Point currPoint : po) {
				Feature ftNode = nodeFeatures.get(currPoint.getX(), currPoint.getY());
				if (ftNode == null) {
					log.warn("no polygon node found for LineString" + id + "!");
					break;
				}
				nodeIds.add((Integer) ftNode.getAttribute(1));
				
				
				MultiPolygon mpNode = (MultiPolygon) ftNode.getDefaultGeometry();
				if (mpNode.getNumGeometries() > 1) {
					log.warn("MultiPolygons with more then 1 Geometry ignored!");
					continue;
				}
				Polygon pNode = (Polygon) mpNode.getGeometryN(0);
				
				Collection<Polygon> separated =  separatePoly(pLink, pNode);
				int found = 0;
				for (Polygon p : separated) {
					if(currLs.crosses(p) || p.contains(currLs) ) {
						//DEBUG
						if (++found > 1) {
							log.warn("found more then 1 matching polygon, possible something is  wrong with pNode ");
							this.pg.createPolygonFeature(p, found, id, 0, 0, 0, 0);
						}
						
						pLink = p;
					}
				}
				
				
				
				
				if (found >1) {
					//DEBUG
					this.pg.createPolygonFeature(pNode, found, id, 0, 0, 0, 0);
					pLink = null;
					break;
				}
				
			}

			if (pLink == null) {
				log.warn("could not create link polygon for LineString:" + id + "!");
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
			if(i > 1) continue;
			
			Feature ftLink = this.pg.getPolygonFeature(pLink, 0, id, nodeIds.get(0), nodeIds.get(1), 0, 0);
			this.ftLinks.put(id, ftLink);
	 	}
		
		
	}
		
	private Collection<Polygon> separatePoly(Polygon pLink , Polygon pNode){
		
		Polygon b = (Polygon) pNode.buffer(0.2);
			Collection<Polygon> p = new ArrayList<Polygon>();
			try {
				Geometry geo = (Geometry) pLink.difference(b);
				for (int i = 0; i<geo.getNumGeometries(); i++) {
					p.add((Polygon) geo.getGeometryN(i));
				}
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return p;		
	}
	
	


}
