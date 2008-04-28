package playground.gregor.shapeFileToMATSim;

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
import org.matsim.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class PolygonGeneratorII {

	private QuadTree<Polygon> polygonTree;
	private QuadTree<Feature> lineTree;
	private HashSet<Polygon> polygons;
	private HashMap<Integer, LineString> lineStrings;
	private FeatureSource featureSourcePolygon;
	private FeatureSource featureSourceLineString;
	private FeatureCollection collectionLineString;
	private List<Feature> featureList;
	private GeometryFactory geofac;
	static final double CATCH_RADIUS = 0.2;
	static final double DEFAULT_DISTANCE = 10;
	private static final Logger log = Logger.getLogger(PolygonGeneratorII.class);
	private boolean graph = false; 
	
	
	private HashMap<Integer, Point> interPoints = new HashMap<Integer, Point>();
	
	public PolygonGeneratorII(FeatureSource ls, FeatureSource po){
		this.featureSourcePolygon = po;
		this.featureSourceLineString = ls;
		this.geofac = new GeometryFactory();
	}
		
	public PolygonGeneratorII(Collection<Feature> graph, FeatureSource po) {
		this.featureSourcePolygon = po;
		this.featureList = (List<Feature>) graph;
		this.geofac = new GeometryFactory();
		this.graph = true;
	}

	public Collection<Feature> generatePolygons() throws Exception{
		log.info("entering polygonGenerator");
		try {
			parsePolygons();
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("leaving polygonGenerator");		
//		return(genPolygonFeatureCollection(mergePolygons()));			
		return(genPolygonFeatureCollection(cutPolygons(lineStrings,mergePolygons())));
		
//		cutPolygons(lineStrings,mergePolygons());
//		return(genPointFeatureCollection(interPoints));
	}
	
	private HashMap<Integer, Polygon> cutPolygons(HashMap<Integer, LineString> lineStrings, HashMap<Integer, Polygon> returnPolys){
		
		int countlines = 0;
		int cCP = 0;
		int countInter = 0;
		
		HashMap<Integer, Polygon> controlPolys = new HashMap<Integer, Polygon>();
		HashMap<Integer, Polygon> debugPolys = new HashMap<Integer, Polygon>();
		

		CoordinateArraySequence s1 = new CoordinateArraySequence(new Coordinate[] {new Coordinate(651048.912,9893489.699)});
		Point p1 = new Point(s1,this.geofac);
		CoordinateArraySequence s2 = new CoordinateArraySequence(new Coordinate[] {new Coordinate(650854.77,9893215.73)});
		Point p2 = new Point(s2,this.geofac);
		
		
		log.info("cutting polygons ...");
		HashMap<Integer, Polygon> fPoly = new HashMap<Integer, Polygon>();
			
		 	for(Iterator<Entry<Integer, LineString>> lsIter = lineStrings.entrySet().iterator() ; lsIter.hasNext() ; ){
		
				Entry<Integer, LineString> lsEntry  = lsIter.next();
				int lsId = lsEntry.getKey();
				LineString currLs = lsEntry.getValue();
 				List<Point> po = new ArrayList<Point>();
				po.add(currLs.getStartPoint());
				po.add(currLs.getEndPoint());
				
				
				
				
				
				
				for(Point currPoint : po){
					
					countlines++;
					log.info(countlines+": " + " lsId: "+lsId + " " +  currLs.getStartPoint().getCoordinate().toString() + " "+ currLs.getEndPoint().getCoordinate().toString());
					
				
					Coordinate currPointCoor = currPoint.getCoordinate();
					List<Feature> lines = (List<Feature>) lineTree.get(currPoint.getX(), currPoint.getY(), CATCH_RADIUS);
					
					if(lines.size()<3){		continue;}//TODO 2 lines abfangen
					
					Polygon poly; 
					if(fPoly.containsKey(lsId)){
						poly = fPoly.get(lsId);
					}else if (returnPolys.containsKey(lsId)) {
						poly  = returnPolys.get(lsId);
					
					
						///////
						Coordinate [] c = currLs.getCoordinates();
						
						for(int i = 0 ; i < c.length-1 ; i++){
							Coordinate [] c1 = new Coordinate[]{c[i],c[i+1]};
							CoordinateSequence seq4 = new CoordinateArraySequence(c1);
							LineString li = new LineString(seq4, geofac);	
						
							if(poly.intersects(li)){
								
								
								Geometry g = poly.intersection(li);
								Coordinate co [] = g.getCoordinates();
								
								for(int ii = 0 ; ii < co.length ; ii++){
									
									Coordinate [] c3 = new Coordinate[]{co[ii]};
									CoordinateSequence seq5 = new CoordinateArraySequence(c3);
									Point p = new Point(seq5, geofac);
									
									
									
									if(!p.within(poly)){
										log.warn(countlines+": LineString intersects polygon "+c.length+" times");
										interPoints.put((countInter), p);
										countInter++;
									}
								}
								
//								continue;
							}
							
							
						}
						/////////
										
					}else {
						log.warn(countlines+": LineString has no poly");
						continue;
					}
					if (lsId == 98) {
						debugPolys.put(61, poly);
					}
					
					
					
					
					
					if (!currPoint.within(poly)){
						log.warn(countlines+": Point is not covered by its polygon");
						if (fPoly.containsKey(lsId)){
							fPoly.remove(lsId);
						}
						continue;
					}
					
					
					
					Coordinate [] coor = poly.getCoordinates();	
					
					List<Point> points = new ArrayList<Point>();				
											
					SortedMap<Double, LineString> sortedLines = sortLines(lines,currPoint);
					
					LineString [] l = new LineString[0];
					LineString [] lineArr = sortedLines.values().toArray(l);
					Double [] d = new Double[0];
					Double [] angleArr = sortedLines.keySet().toArray(d);
					
					for (int i = 0 ; i < lineArr.length ; i++ ){
												
						Polygon controlPoly = getControlPoly(currPointCoor, lineArr, angleArr, i);
						
						controlPolys.put(cCP, controlPoly);
						cCP++;
						
						boolean found = false;
						Envelope o = poly.getEnvelopeInternal();
						QuadTree<Coordinate> polyCoor = new QuadTree<Coordinate>(o.getMinX(), o.getMinY(), o.getMaxX()+o.getMaxX()-o.getMinX(), 
								o.getMaxY()+o.getMaxY()-o.getMinY());
						for(int ii = 0 ; ii < coor.length ; ii++ ){
							polyCoor.put(coor[ii].x, coor[ii].y, coor[ii]);
						}
						while(!found){
							
							Coordinate pp = polyCoor.get(currPointCoor.x, currPointCoor.y);
							
							Coordinate [] cpp = new Coordinate[]{pp};
							CoordinateSequence seq10 = new CoordinateArraySequence(cpp);
							Point cppPoint = new Point(seq10, geofac);
							
							
							if(pp == null || (currPoint.distance(cppPoint) > 10)){
								
								
//								log.warn(countlines+": polyCoor is empty: no point found. ControlPoly: " + controlPoly.toString());
								
								
								
								///////////////////////////////
								LineString lineI = lineArr[i];
								double angleI = angleArr[i];
								LineString lineII;
								double angleII;
								double deltaAngle;
								if (i == ( lineArr.length -1) ) {	
									lineII = lineArr[0];
									angleII = angleArr[0];
									
									if ( (angleII - angleI) <= -180 ){
										deltaAngle = 60;
									}else{
										deltaAngle = 270;
									}

								}else {
									lineII = lineArr[i+1];
									angleII = angleArr[i+1];
									deltaAngle = angleII - angleI;
								}
								
								Coordinate bisecCoor = getBisectingLine(lineII, lineI);
								
								if (deltaAngle > 180 ){
									Coordinate co = subCoord(currPointCoor, bisecCoor); 
									bisecCoor = addCoord(currPointCoor, co);
								}
								///
								
								Coordinate cb = subCoord(currPointCoor, bisecCoor); 
								Coordinate bisecCoorII = addCoord(currPointCoor, cb);
								
								
								Coordinate [] ccc = new Coordinate[]{bisecCoorII, bisecCoor};
								CoordinateSequence seq6 = new CoordinateArraySequence(ccc);
								LineString interLine = new LineString(seq6, geofac);
								
								SortedMap<Double, Coordinate> lengths = new TreeMap<Double, Coordinate>();
								
								Coordinate [] inter = null;
								
//								if(poly.intersects(interLine)){
									
									inter = poly.intersection(interLine).getCoordinates();
									
									
									for(int iii = 0 ; iii < inter.length ; iii++ ){
										lengths.put(inter[iii].distance(currPointCoor),inter[iii]);								
//										log.info(inter[iii].toString());
									}
//								}
								
								
								
								
								for(int iii = 0 ; iii < inter.length ; iii++ ){
									
									Coordinate ppp = lengths.get(lengths.firstKey());
									if(ppp == null){
										log.warn(countlines+": polyCoor is empty: no intersection found.");
										break;
									}
									
									Coordinate [] cccc = new Coordinate[]{currPointCoor, ppp};
									CoordinateSequence seq4 = new CoordinateArraySequence(cccc);
									LineString line2 = new LineString(seq4, geofac);

										if(controlPoly.contains(line2) || line2.crosses(controlPoly)  ){
											Coordinate [] p = new Coordinate[] {lengths.get(lengths.firstKey())};
											CoordinateSequence seqII = new CoordinateArraySequence(p);
											Point poi = new Point(seqII, geofac);
											
											if (pp == null || (currPoint.distance(cppPoint) > currPoint.distance(poi))){
												points.add(poi);
												found = true;
												break;
											}
											
										}else{
											lengths.remove(lengths.firstKey());
										}								
										
										
								
								}
								if (found){break;}
							}
							
							Coordinate [] cc = new Coordinate[]{currPointCoor, pp};
							CoordinateSequence seq2 = new CoordinateArraySequence(cc);
							LineString line = new LineString(seq2, geofac);

								if(controlPoly.contains(line) || line.crosses(controlPoly)  ){ 
									
									Coordinate [] p = new Coordinate[]{pp};

									CoordinateSequence seqII = new CoordinateArraySequence(p);
									Point poi = new Point(seqII, geofac);
									points.add(poi);

									found = true;
									break;
								}else{
									polyCoor.remove(pp.x, pp.y, pp);
								}
						}	
						
					}
					
					if(points.size()<3){
						continue;
					}
					
					HashSet<Polygon> cutPolys = separatePoly(poly, points);
										
					for(Polygon polygon : cutPolys){
						
						if(currLs.crosses(polygon) || polygon.contains(currLs) ){	//
							
//							log.info(polygon.toString());
							
							if(fPoly.containsKey(lsId)){
								fPoly.remove(lsId);
							}
							fPoly.put(lsId, polygon);
							if (p1.equalsExact(po.get(0),0.5) && p2.equalsExact(po.get(1),0.5)) debugPolys.put(61, polygon);
						}						
					}
				}
			}
		log.info("done.");
		return fPoly;
//		return controlPolys;
	}
		
	private HashMap<Integer, Polygon> mergePolygons(){
		
		log.info("merging polygons ...");
		HashMap<Integer, Polygon> returnPolys = new HashMap<Integer, Polygon>();
		
		for (Iterator<Integer> lsIt = lineStrings.keySet().iterator() ; lsIt.hasNext() ; ){
		
			Integer id = (Integer) lsIt.next();
						
			LineString ls = lineStrings.get(id);
						
			HashSet<Polygon> neighborhood = new HashSet<Polygon>();
			Collection<Polygon> polys = polygonTree.get(ls.getCentroid().getX(),ls.getCentroid().getY() , 2000);

			
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
    					if (po.intersects(tmp)||po.touches(tmp)){
    						extNeighborhood.add(po);
    						break;
    					}
    				}
    			}
    		}
			
			if(extNeighborhood.isEmpty()){	continue;}

			Geometry [] gA = new Geometry[0];
   			Geometry [] geoArray = extNeighborhood.toArray(gA);
   			GeometryCollection geoColl = new GeometryCollection(geoArray,geofac);	
   			   			 			
   			try{
   				Geometry retPoly = (Geometry) geoColl.buffer(0.01);
   				if (retPoly.getNumGeometries() > 1) {
   					log.warn("Multipolygon produced in mergePolygons()");
   				}
   				
   				
	   			for (int i = 0; i < retPoly.getNumGeometries(); i++) {
					Polygon polygon = (Polygon) retPoly.getGeometryN(i);
					if(!polygon.isEmpty()){					
						returnPolys.put( id ,polygon);
					}
				}
   			}catch(Exception e){
   				e.printStackTrace();
   			}
   		}

		log.info("done.");
		return returnPolys;
	}
		

	private void parsePolygons()throws Exception{
		
		log.info("parseing features ...");
		
		FeatureCollection collectionPolygon = this.featureSourcePolygon.getFeatures();
		Envelope o = this.featureSourcePolygon.getBounds();
		this.polygons = new HashSet<Polygon>();
		this.polygonTree = new QuadTree<Polygon>(o.getMinX(), o.getMinY(), o.getMaxX(), o.getMaxY());

		FeatureIterator it = collectionPolygon.features();
		while (it.hasNext()) {
			Feature feature = it.next();
			
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
				this.add(polygon);
			}
		}
		
		Iterator iit;
		if(!graph){
			this.collectionLineString = this.featureSourceLineString.getFeatures();
			iit = (Iterator) collectionLineString.features();
		}else{
			iit = featureList.iterator();
		}
		this.lineStrings = new HashMap<Integer, LineString>();
		this.lineTree = new QuadTree<Feature>(o.getMinX(), o.getMinY(), o.getMaxX(), o.getMaxY());
		int id = 0;
		while (iit.hasNext()) {
			Feature feature = (Feature) iit.next();
			
			MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				LineString lineString = (LineString) multiLineString.getGeometryN(i);
				this.add(id, lineString);
				id++;
			}
		}
		
		log.info("done.");
	}

	private void add(Polygon po) {
		this.polygons.add(po);
		Point p = po.getCentroid();
		this.polygonTree.put(p.getX(), p.getY(), po);
	}
	
	private void add(Integer id,  LineString ls) throws Exception {
		this.lineStrings.put(id , ls);
		Feature ft = genLineStringFeature(id, ls);
		Point ep = ls.getEndPoint();
		this.lineTree.put(ep.getX(), ep.getY(), ft);
		Point sp = ls.getStartPoint();
		this.lineTree.put(sp.getX(), sp.getY(), ft);
		
	}
	
	private Collection<Feature> genPolygonFeatureCollection(HashMap<Integer, Polygon> returnPolys) throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType width = AttributeTypeFactory.newAttributeType("width", Double.class);
		AttributeType area = AttributeTypeFactory.newAttributeType("area", Double.class);
		AttributeType info = AttributeTypeFactory.newAttributeType("info", String.class);
		FeatureType ftRoadShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id, width, area, info }, "linkShape");
		
//		for (Iterator<Polygon> it = this.cutPolys.iterator() ; it.hasNext() ; ){	
//			Feature ft = ftRoadShape.create(new Object [] {new MultiPolygon(new Polygon []{(Polygon)it.next()},this.geofac), -1, 0.0, 0.0},"network");
//			features.add(ft);
//		}
				
		for (Iterator<Entry<Integer, Polygon>> it = returnPolys.entrySet().iterator() ; it.hasNext() ; ){	
			Entry<Integer, Polygon> e = it.next();
			Feature ft = ftRoadShape.create(new Object [] {new MultiPolygon(new Polygon []{ e.getValue()  },this.geofac), -1, 0.0, 0.0, e.getKey().toString()},"network");
			features.add(ft);
		}
		return features;
	}
	
	private Feature genLineStringFeature(Integer iid, LineString ls)throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		
		FeatureType ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id }, "linString");
		Feature ft = ftLineString.create(new Object [] {ls, iid},"lineString");
		return ft;
	}
	
	
	private Collection<Feature> genPointFeatureCollection(HashMap<Integer, Point> interPoints)throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		FeatureType ftRoadShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id }, "interPoint");
						
		for (Iterator<Entry<Integer, Point>> it = interPoints.entrySet().iterator() ; it.hasNext() ; ){	
			Entry<Integer, Point> e = it.next();
			Feature ft = ftRoadShape.create(new Object [] { e.getValue() , e.getKey().toString()},"network");
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
		return Math.toDegrees(angle);
		
	}
	
	public static double skalarMultiCoord(Coordinate coordI, Coordinate coordII){
		double skalarprodukt = (coordI.x*coordII.x) + (coordI.y*coordII.y);
		return skalarprodukt;
	}
	
	//Returns the first (start == true) or last (start == false) segment of a lineString
	//The first point of the returned lineString is the start or end point of the original lineString
	private LineString separateLine(LineString ls, boolean start){
		
		LineString vec;
		Coordinate [] lineIcoor = ls.getCoordinates();
		Coordinate [] coor;
		int length = lineIcoor.length;
		
		
		if(lineIcoor[0] == lineIcoor[1]){
			 coor = new Coordinate [length-1];
			 for(int i = 0 ; i < length-1 ; i++){
				 coor[i] = lineIcoor[i+1];
 			 }
			 lineIcoor = coor;
		}
		
		else if(lineIcoor[length -1] == lineIcoor[length -2]){
			 coor = new Coordinate [length-1];
			 for(int i = 0 ; i < length-1 ; i++){
				 coor[i] = lineIcoor[i];
			 }
			 lineIcoor = coor;
		}
		
		
		if(start){
			CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(lineIcoor[0]), new Coordinate(lineIcoor[1])});
			vec = new LineString(seqd, geofac);
		} else {
			
			CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(lineIcoor[lineIcoor.length-1]), new Coordinate(lineIcoor[lineIcoor.length-2])});
			vec = new LineString(seqd, geofac);		
		}
		return vec;
	}
		
	//	LineStrings should have two coordinates. The first should be equal in ls1 and ls2
	private Coordinate getBisectingLine(LineString ls1, LineString ls2){
		
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
	
	private SortedMap<Double, LineString> sortLines(List<Feature> features, Point point){
		
		SortedMap<Double, LineString> sortedLines = new TreeMap<Double, LineString>();
		List<LineString> lines = new ArrayList<LineString>();
		Coordinate [] c = new Coordinate [] {new Coordinate(0,0),new Coordinate(0,1)};
		CoordinateSequence seq = new CoordinateArraySequence(c);
		LineString yLine = new LineString(seq, geofac);
		Coordinate pc = point.getCoordinate();
		
		for (Feature ft : features){			
			
			LineString line = (LineString) ft.getAttribute(0);
			boolean start = line.getStartPoint().equalsExact(point, CATCH_RADIUS);
			LineString vec = separateLine(line, start);
			lines.add(vec);
		}
		for (LineString line : lines){
			
			Coordinate [] co = line.getCoordinates();
			Coordinate [] newCo = new Coordinate [] {new Coordinate(co[0].x - pc.x, co[0].y - pc.y), new Coordinate(co[1].x - pc.x, co[1].y - pc.y)  };
			CoordinateSequence seq2 = new CoordinateArraySequence(newCo);
			LineString li = new LineString(seq2, geofac);
				
			if((co[1].x - pc.x) < 0){
				sortedLines.put((360 - getAngle(li,yLine)), line);
			}else{
				sortedLines.put(getAngle(li,yLine), line);
			}			
		}
		return sortedLines;
	}
	
	private HashSet<Polygon> separatePoly(Polygon poly , List<Point> points){
		
		HashSet<Polygon> pp = new HashSet<Polygon>();
			
			List<Coordinate> pcoor = new ArrayList<Coordinate>();

			for(Point p : points){
				pcoor.add(p.getCoordinate());
			}
			
			if(pcoor.get(0) != pcoor.get(pcoor.size()-1)){
				pcoor.add(pcoor.get(0));
			}
						
			Coordinate [] cos = new Coordinate[0];
			Coordinate [] cosNew = pcoor.toArray(cos);
			CoordinateSequence seq = new CoordinateArraySequence(cosNew);
			LinearRing lr = new LinearRing(seq, geofac);
			Polygon p = new Polygon(lr, null, geofac);
			Polygon b = (Polygon) p.buffer(0.5);
			
			
			if(!poly.contains(p)){
//				log.info("poly touches point: "+ p.toString());
			}
			
			Geometry geo = (Geometry) poly.difference(b);
			
			
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
	

	private Polygon getControlPoly(Coordinate currPointCoor, LineString [] lineArr, Double [] angleArr, int i){
		
		LineString lineI = lineArr[i];
		double angleI = angleArr[i];
		LineString lineII;
		double angleII;
		double deltaAngle;
		if (i == ( lineArr.length -1) ) {	
			lineII = lineArr[0];
			angleII = angleArr[0];
			
			if ( (angleII - angleI) <= -180 ){
				deltaAngle = 60;
			}else{
				deltaAngle = 270;
			}

		}else {
			lineII = lineArr[i+1];
			angleII = angleArr[i+1];
			deltaAngle = angleII - angleI;
		}
		
		Coordinate bisecCoor = getBisectingLine(lineII, lineI);
		
		if (deltaAngle > 180 ){
			Coordinate co = subCoord(currPointCoor, bisecCoor); 
			bisecCoor = addCoord(currPointCoor, co);
		}
								
		Coordinate [] quadcoor = new Coordinate [] {currPointCoor, lineI.getEndPoint().getCoordinate(),
				bisecCoor, lineII.getEndPoint().getCoordinate(), currPointCoor}; 
		CoordinateSequence triseq = new CoordinateArraySequence(quadcoor);
		LinearRing quadRing = new LinearRing(triseq, geofac);
		Polygon controlPoly = new Polygon(quadRing, null, geofac);
//		log.info("controlPoly: "+controlPoly.toString());
		return controlPoly;
	}
}
