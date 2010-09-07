/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonGeneratorII.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

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
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class PolygonGeneratorII {

	private Envelope envelope;
	private QuadTree<Polygon> polygonTree;
	private QuadTree<LineString> lineStringTree;
	private QuadTree<Feature> lineTree;
	private HashSet<Polygon> polygons;
	private HashMap<Integer, LineString> lineStrings;
	private HashMap<Integer, Feature> lineStringFeatures;
	private FeatureSource featureSourcePolygon;
	private FeatureSource featureSourceLineString;
	private FeatureCollection collectionLineString;
	private List<Feature> featureList;
	private Collection<Feature> retPolygons;
	private Collection<Feature> retLineStrings;
	private GeometryFactory geofac;
	static final double CATCH_RADIUS = 0.2;
	static final double DEFAULT_DISTANCE = 10;
	private static final Logger log = Logger.getLogger(PolygonGeneratorII.class);
	private boolean graph = false; 
	
	
	private HashMap<Integer, Point> interPoints = new HashMap<Integer, Point>();
	private FeatureType ftPolygon;
	private FeatureType ftPoint;
	private FeatureType ftLineString;
	private int optid = 0;
	
	public PolygonGeneratorII(FeatureSource ls, FeatureSource po){
		this.featureSourcePolygon = po;
		this.featureSourceLineString = ls;
		this.geofac = new GeometryFactory();
		this.retPolygons = new ArrayList<Feature>();
		this.retLineStrings = new ArrayList<Feature>();
		Envelope o = null;
		try {
			o = this.featureSourcePolygon.getBounds();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.envelope = o;
		this.lineStringTree= new QuadTree<LineString>(0, 0,2* o.getMaxX(), 2*o.getMaxY());
		this.polygons = new HashSet<Polygon>();
		this.polygonTree = new QuadTree<Polygon>(o.getMinX(), o.getMinY(), o.getMaxX() + (o.getMaxX() - o.getMinX()), o.getMaxY() + (o.getMaxY()-o.getMinY()));
		this.lineStrings = new HashMap<Integer, LineString>();
		this.lineStringFeatures = new HashMap<Integer, Feature>();
		this.lineTree = new QuadTree<Feature>(o.getMinX(), o.getMinY(), o.getMaxX() + (o.getMaxX() - o.getMinX()), o.getMaxY() + (o.getMaxY()-o.getMinY()));
//		log.setLevel(Level.ERROR);
		initFeatureGenerator();
	}
		
	public PolygonGeneratorII(Collection<Feature> graph, FeatureSource po) {
		this.featureSourcePolygon = po;
		this.featureList = (List<Feature>) graph;
		this.geofac = new GeometryFactory();
		this.graph = true;
		this.retPolygons = new ArrayList<Feature>();
		this.retLineStrings = new ArrayList<Feature>();
		Envelope o = null;
		try {
			o = this.featureSourcePolygon.getBounds();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.envelope = o;
		this.lineStringTree= new QuadTree<LineString>(o.getMinX()-5000, o.getMinY()-5000, o.getMaxX() + (o.getMaxX() - o.getMinX()), o.getMaxY() + (o.getMaxY()-o.getMinY()));
		this.polygons = new HashSet<Polygon>();
		this.polygonTree = new QuadTree<Polygon>(o.getMinX(), o.getMinY(), o.getMaxX() + (o.getMaxX() - o.getMinX()), o.getMaxY() + (o.getMaxY()-o.getMinY()));
		this.lineStrings = new HashMap<Integer, LineString>();
		this.lineStringFeatures = new HashMap<Integer, Feature>();
		this.lineTree = new QuadTree<Feature>(o.getMinX(), o.getMinY(), o.getMaxX() + (o.getMaxX() - o.getMinX()), o.getMaxY() + (o.getMaxY()-o.getMinY()));
//		log.setLevel(Level.ERROR);
		initFeatureGenerator();
	}
	
	private void initFeatureGenerator(){
		
		AttributeType polygon = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType linestring = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType from = AttributeTypeFactory.newAttributeType("from", Integer.class);
		AttributeType to = AttributeTypeFactory.newAttributeType("to", Integer.class);
		AttributeType width = AttributeTypeFactory.newAttributeType("min_width", Double.class);
		AttributeType area = AttributeTypeFactory.newAttributeType("area", Double.class);
		AttributeType length = AttributeTypeFactory.newAttributeType("length", Double.class);
		AttributeType info = AttributeTypeFactory.newAttributeType("info", String.class);
		try {
			this.ftPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {polygon, id, from, to, width, area, length }, "linkShape");
			this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {point, id, info }, "pointShape");
			this.ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {linestring, id, info }, "linString");			
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
	}

	public Collection<Feature> generatePolygons() throws Exception{
		log.info("entering polygonGenerator");
		try {
			parsePolygons();
		} catch (Exception e) {
			e.printStackTrace();
		}

		
//		return(genPolygonFeatureCollection(mergePolygons()));	
//		HashMap<Integer, Polygon> tmpPolygons = mergePolygons();
//		HashMap<Integer, Polygon> merged = new PolygonMerger("./padang/converter/d_p_merged.shp").getMergedPolygons();
		HashMap<Integer, Polygon> merged = new PolygonMerger(this,"./padang/converter/d_p_merged.shp").getMergedPolygons();
//		HashMap<Integer, Polygon> merged = new PolygonMerger(this).getMergedPolygons();
//		HashMap<Integer, Polygon> merged = new PolygonMerger("./padang/converter/d_p_merged.shp").getMergedPolygons();
		
		
//		QuadTree<Polygon> polygonNodes = getPolygonNodes(merged);
//		QuadTree<Feature> nodeFeatures = new PolygonNodesGenerator(this,"./padang/converter/d_p_nodes.shp").getPolygonNodes(merged);
		QuadTree<Feature> nodeFeatures = new PolygonNodesGenerator(this,"./padang/converter/d_p_nodes.shp").getPolygonNodes(merged);
//		QuadTree<Feature> nodeFeatures = new PolygonNodesGenerator("./padang/converter/d_p_nodes.shp").getPolygonNodes(merged);
		
		new PolygonLinksGenerator(this, "./padang/converter/d_p_links.shp").getPolygonLinks(nodeFeatures,merged);
		
//		cutPolygons(this.lineStrings,);
//		createPolygonFeatures(cutPolygons(this.lineStrings,mergePolygons()));
//		createPolygonFeatures(mergePolygons());
		log.info("saving debugging stuff");
		ShapeFileWriter.writeGeometries(this.retLineStrings, "./padang/converter/debugg_ls.shp");
		ShapeFileWriter.writeGeometries(this.retPolygons, "./padang/converter/debugg_p.shp");
		
		log.info("done.");
		log.info("leaving polygonGenerator");		
		return this.retPolygons;
		
	
//		cutPolygons(lineStrings,mergePolygons());
//		return(genPointFeatureCollection(interPoints));
	}




	


	private void parsePolygons()throws Exception{
		
		log.info("parseing features ...");
		
		FeatureCollection collectionPolygon = this.featureSourcePolygon.getFeatures();
	
		
		
		log.info("\t-PolygonString");
		FeatureIterator it = collectionPolygon.features();
		while (it.hasNext()) {
			Feature feature = it.next();
			
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
				try {
					polygon = (Polygon) polygon.buffer(0.01);
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.add(polygon);
			}

		}
		log.info("\t-LineString");		
	
		if(!graph){
			this.collectionLineString = this.featureSourceLineString.getFeatures();
			it = collectionLineString.features();
			while (it.hasNext()) {
				Feature feature = it.next();
				int id = (Integer) feature.getAttribute(1);
				MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
				
				if (multiLineString.getNumGeometries() > 1) {
					throw new RuntimeException("only one LineString is allowed per MultiLineString");
				}
				LineString lineString = (LineString) multiLineString.getGeometryN(0);
				if (lineString.getNumPoints() <= 1) {
					log.warn("ls consists of <= 1 point! This should not Happen!!");
					continue;
				}
				this.add(id, lineString,feature);
				
				
//				for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
//					LineString lineString = (LineString) multiLineString.getGeometryN(i);
//					this.add(id, lineString);
//					id++;
//				}
			}
		}else{
			Iterator iit = featureList.iterator();

			while (iit.hasNext()) {
				Feature feature = (Feature) iit.next();
				int id = (Integer) feature.getAttribute(1);
				MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
				
				if (multiLineString.getNumGeometries() > 1) {
					throw new RuntimeException("only one LineString is allowed per MultiLineString");
				}
				LineString lineString = (LineString) multiLineString.getGeometryN(0);
				if (lineString.getNumPoints() <= 1) {
					log.warn("ls consists of <= 1 point! This should not Happen!!");
					continue;
				}
				this.add(id, lineString,feature);
				
				
//				for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
//					LineString lineString = (LineString) multiLineString.getGeometryN(i);
//					this.add(id, lineString);
//					id++;
//				}
			}
		}

		
		log.info("done.");
	}

	private void add(Polygon po) {
		this.polygons.add(po);
		Point p = po.getCentroid();
		this.polygonTree.put(p.getX(), p.getY(), po);
	}
	
	private void add(Integer id,  LineString ls, Feature feature) throws Exception {
		this.lineStrings.put(id , ls);
		this.lineStringFeatures.put(id,feature);
		
		Point ep = ls.getEndPoint();
		this.lineTree.put(ep.getX(), ep.getY(), feature);
		Point sp = ls.getStartPoint();
		this.lineTree.put(sp.getX(), sp.getY(), feature);
		this.lineStringTree.put(ep.getX(), ep.getY(), ls);
		this.lineStringTree.put(sp.getX(), sp.getY(), ls);
		
	}

	
	////////////////////////////////////////////////////////////
	// feature stuff
	///////////////////////////////////////////////////////////
	
	
	void createPolygonFeature(Polygon polygon, double length, int id, int from, int to, double min_width, double area) {
		
		this.retPolygons.add(getPolygonFeature(polygon, length, id, from, to,min_width,  area));
		log.info("created new debug polygon with id " + id);
	}
	
	public Feature getPolygonFeature(Polygon polygon, double length, int id, int from, int to,double min_width, double area ){
		Feature ft = null;
		try {
			ft = this.ftPolygon.create(new Object [] {new MultiPolygon(new Polygon []{ polygon  },this.geofac), id, from, to, min_width, area, length},"network");
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}		
		return ft;
	}
	
	void createLineStringFeature(LineString ls, int id, String info) {
		this.retLineStrings.add(getLineStringFeature(ls,id,info));
//		log.info("created new debug ls with id " + id);
		
	}
	
	private Feature getLineStringFeature(LineString ls, int id, String info) {
		Feature ft = null;
		try {
			ft = this.ftLineString.create(new Object[] {ls,id,info});
		} catch (IllegalAttributeException e1) {
			e1.printStackTrace();
		}
		return ft;
	}

	private void createPolygonFeatures(HashMap<Integer, Polygon> polygons) {

		try {
			for (Iterator<Entry<Integer, Polygon>> it = polygons.entrySet().iterator() ; it.hasNext() ; ){	
				Entry<Integer, Polygon> e = it.next();
				Feature ft = this.ftPolygon.create(new Object [] {new MultiPolygon(new Polygon []{ e.getValue()  },this.geofac), e.getKey().toString(), 0.0, 0.0, 0},"network");
				this.retPolygons.add(ft);
			}
		} catch (IllegalAttributeException e1) {
			e1.printStackTrace();
		}
	}
	

	private Feature genLineStringFeature(Integer iid, LineString ls, int info)throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		return this.ftLineString.create(new Object [] {ls, iid,info},"lineString");
	}
	
	
	private Collection<Feature> genPointFeatureCollection(HashMap<Integer, Point> interPoints)throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		Collection<Feature> features = new ArrayList<Feature>();
		for (Iterator<Entry<Integer, Point>> it = interPoints.entrySet().iterator() ; it.hasNext() ; ){	
			Entry<Integer, Point> e = it.next();
			Feature ft = this.ftPoint.create(new Object [] { e.getValue() , e.getKey().toString()},"network");
			features.add(ft);
		}
		return features;
	}
	
		
//	private double calcWitdh(LineString ls, List<Polygon> po){
//		
//		
//		List<Double> widths = new ArrayList<Double>();
//		Coordinate [] linkCoord = ls.getCoordinates();	
//									
//		//Breiten ueber Linienpunkte 
//		
//		for(int i=0 ; i < linkCoord.length ; i++){												
//			
//			Coordinate [] g = new Coordinate[]{linkCoord[i]};
//			
//			CoordinateSequence seq = new CoordinateArraySequence(g);
//    		Point point = new Point(seq,geofac);
//			
//    		Iterator<Polygon> polygonIterator = po.iterator();
//    		
//			
//				while(polygonIterator.hasNext()){
//						
//						Polygon polygonGeo = polygonIterator.next();
//						Coordinate coord1;
//						Coordinate coord2;
//						
//						if(i==0){
//							coord1 = new Coordinate(linkCoord[i]);
//							coord2 = new Coordinate(linkCoord[i+1]);
//						} else {
//							coord1 = new Coordinate(linkCoord[i]);
//							coord2 = new Coordinate(linkCoord[i-1]);
//						}
//						
//						Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));
//						
//						Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
//						Coordinate coordA = subCoord(coord1, multiCoord(ortho, 100));
//						Coordinate coordB = subCoord(coord1, multiCoord(ortho, -100));
//									
//						Coordinate [] d = new Coordinate[]{coordA, coordB};
//						CoordinateSequence seqd = new CoordinateArraySequence(d);
//						LineString orthoLine = new LineString(seqd, geofac);			
//														
//						if(polygonGeo.intersects(orthoLine)){
//							Coordinate [] inter = polygonGeo.intersection(orthoLine).getCoordinates();
//							for(int iii = 0 ; iii < inter.length-1 ; iii++ ){
//								widths.add(getLength(subCoord(inter[iii],inter[iii+1])));																			
//							}
//									
//						}																				
//					}
//				}
//			
//		//Breiten ueber Polygonpunkte
//		
//		for(Iterator<Polygon> it = po.iterator() ; it.hasNext() ; ){
//			
//			Polygon polygonGeo = it.next();
//			Coordinate [] polyCoord = polygonGeo.getCoordinates();
//
//			for(int i=0 ; i<polyCoord.length-1 ; i++){																		
//				
//				Coordinate polyCoordX = polyCoord[i];
//				
//				for(int ii=0 ; ii<linkCoord.length-1 ; ii++){
//					
//					Coordinate coord1 = new Coordinate(linkCoord[ii]);
//					Coordinate coord2 = new Coordinate(linkCoord[ii+1]);
//												
//					Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));
//					Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
//					
//					Coordinate coordA = subCoord(polyCoordX, multiCoord(ortho, 100));
//					Coordinate coordB = subCoord(polyCoordX, multiCoord(ortho, -100));
//					
//					Coordinate [] d = new Coordinate[]{coordA, coordB};
//					CoordinateSequence seqd = new CoordinateArraySequence(d);
//					LineString orthoLine = new LineString(seqd, geofac);
//					
//					Coordinate [] e = new Coordinate[]{coord1, coord2};
//					CoordinateSequence seqe = new CoordinateArraySequence(e);
//					LineString line = new LineString(seqe, geofac);
//													
//					if(polygonGeo.intersects(orthoLine) && orthoLine.intersects(line)){
//						try{
//							Coordinate [] inter = polygonGeo.intersection(orthoLine).getCoordinates();
//							for(int iii = 0 ; iii < inter.length-1 ; iii++ ){
//								widths.add(getLength(subCoord(inter[iii],inter[iii+1])));									
//							}
//						}catch(com.vividsolutions.jts.geom.TopologyException e1){
//							log.info("TopologyException [Geometry.intersection()]: link:");
//						}catch(Exception e1){
//							log.info(e1);
//						}
//						
//					}																					
//				}
//			}					
//		}
//		
//		//Breite
//		double minWidth = 100;
//		for(Iterator<Double> it = widths.iterator() ; it.hasNext() ; ){					
//			Double tmpWidth = (Double) it.next();
//			double tmpWidthValue = tmpWidth.doubleValue();
//			if(tmpWidthValue < minWidth)minWidth = tmpWidthValue;
//		}
//		
//	return minWidth;	
//	}
	

		
	//	LineStrings should have two coordinates. The first should be equal in ls1 and ls2
	public static Coordinate getBisectingLine(LineString ls1, LineString ls2){
		
		Coordinate r5;
		
//		if (getAngle(ls1, ls2) == 180){
//			
//			Coordinate c1 = subCoord(ls1.getCoordinates()[1],ls1.getCoordinates()[0]);
//			
//			Coordinate c = new Coordinate( - c1.y , c1.x );
//			Coordinate c2 = multiCoord(addCoord(ls1.getCoordinates()[0], c), (1/getLength(c1)));
//			r5 = multiCoord(c2, 50);
//			
//		}else{
			Coordinate c1 = subCoord(ls1.getCoordinates()[1],ls1.getCoordinates()[0]);
			Coordinate c2 = subCoord(ls2.getCoordinates()[1],ls2.getCoordinates()[0]);
			double c1Length = getLength(c1); 
			double c2Length = getLength(c2);
			Coordinate c11 = multiCoord(c1,(1/c1Length));
			Coordinate c22 = multiCoord(c2,(1/c2Length));
			Coordinate r = 	multiCoord(subCoord(c22,c11),0.5);
			Coordinate r2 = addCoord(ls1.getCoordinates()[0], c11);
			Coordinate r3 = addCoord(r2, r);
			Coordinate r4; 
			
			
			if(getAngle(ls1, ls2) == 180){
				r4 = new Coordinate( - c1.y , c1.x);
			} else {
				r4 = subCoord(r3 , ls1.getCoordinates()[0]);
			}
		
			
//			if(getLength(r4) < 1){
				r4 = multiCoord(r4, (1/getLength(r4))*50);  //Math.min(c1Length, c2Length)
//			}
				 
			r5 = addCoord(ls1.getCoordinates()[0], r4);  //multiCoord(r4, ((c1Length + c2Length)/2))
//		}
//			if (r5 == null){
//				
//				Coordinate c = new Coordinate(0,0);
//				
//				return c;
//			}
//		log.info(r5.toString());
		return r5;
	}
	
	
	
	private HashSet<Polygon> separatePoly(Polygon poly , List<Point> points){
		
		HashSet<Polygon> pp = new HashSet<Polygon>();
			
			List<Coordinate> pcoor = new ArrayList<Coordinate>();
			
			for(Point p : points){
				pcoor.add(p.getCoordinate());
			}
			
			if(!pcoor.get(0).equals(pcoor.get(pcoor.size()-1))){
				pcoor.add(pcoor.get(0));
			}
			
//			Coordinate [] cos = new Coordinate[0];
			
			Coordinate [] cos = pcoor.toArray(new Coordinate[pcoor.size()]);
			LinearRing lr = this.geofac.createLinearRing(cos);
			Polygon p = this.geofac.createPolygon(lr, null);
			Polygon b = (Polygon) p.buffer(0.5);
//			createPolygonFeature(b, -1);
			
			
//			if(!poly.contains(p)){
//				log.info("poly touches point: "+ p.toString());
//			}
			
			Geometry geo = poly.difference(b);
			
			
			if (geo.getNumGeometries() == 1){
				pp.add((Polygon) geo);
			}else{
				
				MultiPolygon mPoly = (MultiPolygon) geo;
				for (int i = 0; i < mPoly.getNumGeometries(); i++) {
					Polygon polygon = (Polygon) mPoly.getGeometryN(i);
					pp.add(polygon);
				}
			}
			
	
			
			return pp;		
	}
	
	

//	private HashSet<Polygon> separatePolyII(Polygon poly, List<Point> points){
//		
//		Coordinate [] coords = poly.getCoordinates();
//		HashSet<Polygon> cutPolys = new HashSet<Polygon>();
//				
//		for(int it1 = 0 ; it1 < points.size() ; it1++){        
//			
//			List<Coordinate> newPoly = new ArrayList<Coordinate>();
//			List<Coordinate> restPoly = new ArrayList<Coordinate>();
//			
//			int countPoints = 0;
//						
//			for (int i = 0 ; i < coords.length ; i++){
//				CoordinateSequence seq = new CoordinateArraySequence(new Coordinate []{coords[i]});
//				Point p = new Point(seq, geofac);
//			
//				if (countPoints == 0 || countPoints == points.size()){
//					
//					newPoly.add(coords[i]);				
//					for (Iterator<Point> it = points.iterator() ; it.hasNext() ; ){
//						Point p1 = (Point) it.next();
//						if ((p.equalsExact(p1, CATCH_RADIUS)) && (i != 0)){
//							restPoly.add(coords[i]);						
//							countPoints++;
//							break;
//						}
//					}
//				}else{
//					restPoly.add(coords[i]);
//					for (Iterator<Point> it = points.iterator() ; it.hasNext() ; ){
//						Point p1 = (Point) it.next();
//						
//						if ((countPoints == (points.size()-1)) && (p.equalsExact(p1, CATCH_RADIUS))){
//							newPoly.add(coords[i]);
//							countPoints++;
//							break;
//						}else if (p.equalsExact(p1, CATCH_RADIUS)){
//							countPoints++;
//						}
//					}
//				}
//			}
//			if (newPoly.get(0) != newPoly.get(newPoly.size()-1)){
//				log.warn("newpoly is not closed!");
//				newPoly.add(newPoly.get(0));
//			}
//			
//			if ((!restPoly.isEmpty()) && restPoly.get(0) != restPoly.get(restPoly.size()-1)){
//				log.info("restpoly is not closed!");
//				restPoly.add(restPoly.get(0));
//			}
//			
//			Coordinate [] co = new Coordinate[0];
//			Coordinate [] coNew = newPoly.toArray(co);
//			CoordinateSequence seq = new CoordinateArraySequence(coNew);
//
//			try{
//				LinearRing lr = new LinearRing(seq, geofac);
//				Polygon p = new Polygon(lr, null, geofac);
//				cutPolys.add(p);
//			}catch(Exception e){
//				
//				log.warn("linearRing" + seq);
//			}
//				
//			coords = restPoly.toArray(co);
//		}
//		
//		return cutPolys;
//	}
	


/////////////////////////////////////////////////////////
// calc (public)
/////////////////////////////////////////////////////////
	
	/* package */ protected Polygon addVertex(Polygon poly, Point v) {
		
		LinearRing  [] lrs = null;
		
		if (poly.getNumInteriorRing() > 0) {
			lrs = new LinearRing [poly.getNumInteriorRing()];
			for (int i = 0; i< poly.getNumInteriorRing(); i++) {
				lrs[i] = (LinearRing) poly.getInteriorRingN(i);
			}
		}
		
		
		
		
		LineString ls1 = poly.getExteriorRing();
		LineString ls2 =addVertexToLineString(ls1,v,0.1);
		
		if ((lrs != null) && (ls1.getNumPoints() == ls2.getNumPoints())) { //No matching position found so far
			for (int i=0; i < lrs.length; i++){
				LineString tmp = addVertexToLineString(lrs[i],v,0.1);

				if (tmp.getNumPoints() > lrs[i].getNumPoints()) {
					lrs[i] = this.geofac.createLinearRing(tmp.getCoordinates());
					break;
				}
			}
		}
				
		return this.geofac.createPolygon(this.geofac.createLinearRing(ls2.getCoordinates()),lrs);
	}
	
	/* package */ protected LineString addVertexToLineString(LineString ls, Point p, double min_dist) {
		
		Coordinate [] coords = new Coordinate [ls.getNumPoints()+1];
		coords[0] = ls.getStartPoint().getCoordinate();
		int n = 1;
		boolean notFound = true;
		for (int i =1; i < ls.getNumPoints() ; i ++) {
			LineString seg = this.geofac.createLineString(new Coordinate[] {ls.getPointN(i-1).getCoordinate(),ls.getPointN(i).getCoordinate() });
			if ((seg.distance(p) < min_dist) && notFound) {
				notFound = false;
				coords[n++] = p.getCoordinate();
			} 
			coords[n++] = ls.getPointN(i).getCoordinate();
		}
		if (notFound) {
			return ls;
		}
		
		return this.geofac.createLineString(coords);
	}
	
	

public QuadTree<Coordinate> getCoordinateQuatTree(Polygon p) {
	QuadTree<Coordinate> q = new QuadTree<Coordinate>(0, 0, this.envelope.getMaxX() + (this.envelope.getMaxX() - this.envelope.getMinX()), this.envelope.getMaxY() + (this.envelope.getMaxY()-this.envelope.getMinY()));
	for (int i = 0; i < p.getExteriorRing().getNumPoints() -1; i++) {
		Coordinate c = p.getExteriorRing().getCoordinateN(i);
		q.put(c.x, c.y, c);
	}

	for (int i = 0; i < p.getNumInteriorRing(); i++) {
		for (int j = 0; j < p.getInteriorRingN(i).getNumPoints(); j++) {
			Coordinate c = p.getInteriorRingN(i).getPointN(j).getCoordinate();
			q.put(c.x, c.y, c);
		}
		
		
	}
		return q;
}

/*package*/ protected SortedMap<Double, LineString> sortLines(Collection<LineString> ls, Point point) throws Exception{
	
	SortedMap<Double, LineString> sortedLines = new TreeMap<Double, LineString>();
	List<LineString> lines = new ArrayList<LineString>();
	Coordinate [] c = new Coordinate [] {new Coordinate(0,0),new Coordinate(0,1)};
	CoordinateSequence seq = new CoordinateArraySequence(c);
	LineString yLine = new LineString(seq, this.geofac);
	Coordinate pc = point.getCoordinate();
	
	for (LineString l : ls){			
		
		
		boolean start = l.getStartPoint().equalsExact(point, PolygonGeneratorII.CATCH_RADIUS);
		LineString vec = separateLine(l, start, point.getCoordinate());
		lines.add(vec);
	}
	for (LineString line : lines){
		
		Coordinate [] co = line.getCoordinates();
		Coordinate [] newCo = new Coordinate [] {new Coordinate(co[0].x - pc.x, co[0].y - pc.y), new Coordinate(co[1].x - pc.x, co[1].y - pc.y)  };
		CoordinateSequence seq2 = new CoordinateArraySequence(newCo);
		LineString li = new LineString(seq2, this.geofac);
			
		if((co[1].x - pc.x) < 0){
			sortedLines.put((360 - PolygonGeneratorII.getAngle(li,yLine)), line);
		}else{
			sortedLines.put(PolygonGeneratorII.getAngle(li,yLine), line);
		}			
	}
	return sortedLines;

}

public static Coordinate multiCoord(Coordinate coordI, double skalar){
	Coordinate coord = new Coordinate(coordI.x*skalar, coordI.y*skalar);
	return coord;
}

public static Coordinate subCoord(Coordinate coordI, Coordinate coordII){
	Coordinate coord = new Coordinate(coordI.x - coordII.x, coordI.y - coordII.y);
	return coord;
}

public static double getLength(Coordinate coordI){
	double length = Math.sqrt((coordI.x*coordI.x)+(coordI.y*coordI.y));
	return length;
}

public static double getAngle(Coordinate coordI, Coordinate coordII){				
	double angle = Math.acos(skalarMultiCoord(coordI,coordII)/(getLength(coordI)*getLength(coordII)));
	return angle;
}

public static Coordinate addCoord(Coordinate coordI, Coordinate coordII){
	Coordinate coord = new Coordinate(coordI.x + coordII.x, coordI.y + coordII.y);
	return coord;
}

//Only works for lineStrings with one segment
public static double getAngle(LineString lI, LineString lII){
	Coordinate [] cI = lI.getCoordinates();
	Coordinate [] cII = lII.getCoordinates();
	Coordinate coordI = subCoord(cI[1],cI[0]);
	Coordinate coordII = subCoord(cII[1],cII[0]);
	
	double angle = Math.acos(skalarMultiCoord(coordI,coordII)/(getLength(coordI)*getLength(coordII)));
	if (Double.isNaN(angle)){//TODO ist das richtig???
		return 0.;
	}
	return Math.toDegrees(angle);
	
}

public static double skalarMultiCoord(Coordinate coordI, Coordinate coordII){
	double skalarprodukt = (coordI.x*coordII.x) + (coordI.y*coordII.y);
	return skalarprodukt;
}

//Returns the first (start == true) or last (start == false) segment of a lineString
//The first point of the returned lineString is the start or end point of the original lineString
public LineString separateLine(LineString ls, boolean start, Coordinate c) throws Exception{
	
	LineString vec;
	Coordinate [] lineIcoor = ls.getCoordinates();
	Coordinate [] coor;
	int length = lineIcoor.length;
	
	
	if(lineIcoor[0].distance(lineIcoor[1]) <= 0.5){
		 coor = new Coordinate [length-1];
		 for(int i = 0 ; i < length-1 ; i++){
			 coor[i] = lineIcoor[i+1];
			 }
		 lineIcoor = coor;
	}

	else if(lineIcoor[length -1].distance(lineIcoor[length -2]) <= 0.5 ){
		 coor = new Coordinate [length-1];
		 for(int i = 0 ; i < length-1 ; i++){
			 coor[i] = lineIcoor[i];
		 }
		 lineIcoor = coor;
	}
	
	
	if(start){
//		CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(lineIcoor[0]), new Coordinate(lineIcoor[1])});
		CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(c), new Coordinate(lineIcoor[1])});
		vec = new LineString(seqd, geofac);
	} else {
		
//		CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(lineIcoor[lineIcoor.length-1]), new Coordinate(lineIcoor[lineIcoor.length-2])});
		CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(c), new Coordinate(lineIcoor[lineIcoor.length-2])});
		vec = new LineString(seqd, geofac);		
	}
	return vec;
}




	/////////////////////////////////////////////////////////
	// getter
	/////////////////////////////////////////////////////////
	public HashMap<Integer, LineString> getLineStringsMap() {
		return this.lineStrings;
	}
	public HashMap<Integer, Feature> getLineStringFeatures() {
		return this.lineStringFeatures;
	}
	
	public QuadTree<LineString> getLineStringsQuadTree() {
		return this.lineStringTree;
	}
	
	
	public QuadTree<Polygon> getPolygonQuadTree() {
		return this.polygonTree;
	}

	public GeometryFactory getGeofac() {
		return this.geofac;
	}

	public QuadTree<LineString> getLineStringQuadTree() {
		return this.lineStringTree;
	}

	public Envelope getEnvelope() {
		return this.envelope;
	}
	
	
}
