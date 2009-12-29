/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonNodesGenerator.java
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
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class PolygonNodesGenerator {

	private static final Logger log = Logger.getLogger(PolygonNodesGenerator.class);
	
	private PolygonGeneratorII pg;
	private boolean readFromFile = false;
	private String file;


	private boolean saveToFile = false;
	private QuadTree<Feature> nodes;
	
	
	public PolygonNodesGenerator(PolygonGeneratorII pg) {
		this.pg = pg;
	}
	public PolygonNodesGenerator(PolygonGeneratorII pg, String file){
		this.pg = pg;
		this.saveToFile  = true;
		this.file = file;
	}
	
	public PolygonNodesGenerator(String file){
		this.readFromFile = true;
		this.file = file;
	}
	
	
	public QuadTree<Feature> getPolygonNodes(HashMap<Integer, Polygon> mergedPolygons) throws Exception {
		
		if (this.readFromFile) {
			readFromFile();
		} else {
			createPolygonNodes(mergedPolygons);
		}
		if (this.saveToFile) {
			ShapeFileWriter.writeGeometries(this.nodes.values(), this.file);

		}
		
		
		return this.nodes;
		
	}

	private void readFromFile() throws Exception {
		
		FeatureSource ft = ShapeFileReader.readDataFile(this.file);
		Envelope e = ft.getBounds();
		this.nodes = new QuadTree<Feature>(e.getMinX(),e.getMinY(), e.getMaxX(), e.getMaxY());
		FeatureIterator it = ft.getFeatures().features();
		while (it.hasNext()) {
			Feature feature = it.next();
			double x = (Double)feature.getAttribute(4);
			double y = (Double)feature.getAttribute(5);
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
				continue;
			}
			this.nodes.put(x, y, feature);
		}
	}
	
	
	
	private void createPolygonNodes(HashMap<Integer, Polygon> mergedPolygons) {
		log.info("generating polygon nodes ...");
		this.nodes = new QuadTree<Feature>(this.pg.getEnvelope().getMinX(), this.pg.getEnvelope().getMinY(), this.pg.getEnvelope().getMaxX() + (this.pg.getEnvelope().getMaxX() - this.pg.getEnvelope().getMinX()), this.pg.getEnvelope().getMaxY() + (this.pg.getEnvelope().getMaxY()-this.pg.getEnvelope().getMinY()));
		Integer nodeId = 0;
	
	 	for(Iterator<Entry<Integer, LineString>> lsIter = this.pg.getLineStringsMap().entrySet().iterator() ; lsIter.hasNext() ; ){
			
	 		
			Entry<Integer, LineString> lsEntry  = lsIter.next();
			int lsId = lsEntry.getKey();

			LineString currLs = lsEntry.getValue();
//			if (currLs.getLength() < 8)	this.pg.createLineStringFeature(currLs, lsId, "");
			List<Point> po = new ArrayList<Point>();
			po.add(currLs.getStartPoint());
			po.add(currLs.getEndPoint());
			
			nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(2);
			
			for (Point currPoint : po) {

				
				
				if (this.nodes.get(currPoint.getX(), currPoint.getY(), PolygonGeneratorII.CATCH_RADIUS).size() > 0) {
					nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(3);
					continue;
				}
				
				Collection<LineString> tmpLs = this.pg.getLineStringQuadTree().get(currPoint.getX(), currPoint.getY(), PolygonGeneratorII.CATCH_RADIUS);
				
				if (tmpLs.size() == 1) {
					//seems to be a dead end node
					Polygon deadendp = getDeadEndNode(currPoint);
					Feature ft = this.pg.getPolygonFeature(deadendp, 0, nodeId, 0, 0, currPoint.getX(), currPoint.getY());
					this.nodes.put(currPoint.getX(), currPoint.getY(), ft);
					nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(3);
				}
				
				
				
				
				
				if (tmpLs.size() <= 2) {
					continue;
				}
				
				Polygon tmpPoly; 
				if(mergedPolygons.containsKey(lsId)){
					tmpPoly = mergedPolygons.get(lsId);
				}else  {
					log.warn("No corresponding Polygon found for LineString: " + lsId + " !");
//					this.pg.createLineStringFeature(currLs, lsId, "");
					break;
				}
				
				
				SortedMap<Double, LineString> sortedLs = null;
				try {
					sortedLs = this.pg.sortLines(tmpLs,currPoint);
				} catch (Exception e) {
					log.warn("could not sort line strings");
					nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(3);
					continue;
				}
				
				
				
				
				
				TreeMap<Double,LineString> sortedLsTree = new TreeMap<Double,LineString>(sortedLs);
				if (sortedLsTree.values().size() < 3) {
					log.warn("intersection with only: " + sortedLsTree.values().size() + " LineString found, this should not happen!" );
//					this.pg.createLineStringFeature(currLs, lsId, "");
					nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(3);
					continue;
				}
//				Coordinate [] nodeC = new Coordinate[sortedLsTree.values().size()+1];
				ArrayList<Coordinate> nodeC = new ArrayList<Coordinate>();

				for (double angle : sortedLs.keySet()) {
					Polygon p = getControlPolygon(currPoint, angle,sortedLsTree);
					
//					try {
//						//DEBUG
//						this.pg.createPolygonFeature(p, -10,lsId, 0, 0, 0., 0.);
//					} catch (RuntimeException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} 
	
					if (p == null) {
						break;
					}
//					createPolygonFeature(p, 2, lsId);
					QuadTree<Coordinate> tmpPolyQ = this.pg.getCoordinateQuatTree(tmpPoly);	
					boolean found = false;
					Coordinate c = null;
					while (!found && tmpPolyQ.values().size() > 0) {
						c = tmpPolyQ.get(currPoint.getX(), currPoint.getY());
						LineString tmptmpl  = this.pg.getGeofac().createLineString(new Coordinate [] {currPoint.getCoordinate(), c});
						if (p.contains(tmptmpl) || tmptmpl.crosses(p)) {
							found = true;
						} else {
							tmpPolyQ.remove(c.x, c.y, c);
						}

					}
					if (found) {
						nodeC.add(c);
					} 
				}
				if (nodeC.size() < 3) {
//					log.warn("could not craete polygon node for ls:" + lsId);
					nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(3);
					continue;
				}
				nodeC.add(nodeC.get(0));
				Coordinate [] nodeCa = new Coordinate [nodeC.size()];
				nodeC.toArray(nodeCa);
				LinearRing lr = this.pg.getGeofac().createLinearRing(nodeCa);
				Polygon p = this.pg.getGeofac().createPolygon(lr, null);
			
				
				Feature ft = this.pg.getPolygonFeature(p, 0, nodeId, 0, 0, currPoint.getX(), currPoint.getY());
				this.nodes.put(currPoint.getX(), currPoint.getY(), ft);
				nodeId = (Integer) this.pg.getLineStringFeatures().get(lsId).getAttribute(3);
				//DEBUG
//				this.pg.createPolygonFeature(p, 1, lsId,0,0,0,0);

				
				
			}
			
			
	 	}
		
		
		log.info("done.");
	}

	

	private Polygon getDeadEndNode(Point currPoint) {
		Coordinate c0 = currPoint.getCoordinate();
		Coordinate [] coords = new Coordinate [] {c0, new Coordinate(c0.x+0.01,c0.y), new Coordinate(c0.x,c0.y+0.01), c0}; 
		
		return this.pg.getGeofac().createPolygon(this.pg.getGeofac().createLinearRing(coords), null);
	}
	private Polygon getControlPolygon(Point p, double angle1, TreeMap<Double, LineString> sortedLs) {
		
		LineString ls1 = sortedLs.get(angle1);
		Entry<Double, LineString> e = sortedLs.higherEntry(angle1);
		if (e == null) {
			e = sortedLs.firstEntry();
		}
		LineString ls2 = e.getValue();
		double angle2 = e.getKey();
		double dA = angle2 - angle1;
		dA = dA > 0  ? dA : 360 + dA;
		
		Coordinate bisecCoor = PolygonGeneratorII.getBisectingLine(ls1, ls2);
		
		if (dA > 180 ){
			Coordinate co = PolygonGeneratorII.subCoord(p.getCoordinate(), bisecCoor); 
			bisecCoor = PolygonGeneratorII.addCoord(p.getCoordinate(), co);
		}
								
		Coordinate [] quadcoor = new Coordinate [] {p.getCoordinate(), ls1.getEndPoint().getCoordinate(),
				bisecCoor, ls2.getEndPoint().getCoordinate(), p.getCoordinate()}; 
		CoordinateSequence triseq = new CoordinateArraySequence(quadcoor);
		LinearRing quadRing = new LinearRing(triseq, this.pg.getGeofac());
		Polygon controlPoly = new Polygon(quadRing, null, this.pg.getGeofac());
//		log.info("controlPoly: "+controlPoly.toString());
		if (!controlPoly.isValid()) { //TODO das darf nicht passieren
			return null;
		}
		return controlPoly;

	}
	
}
