package playground.gregor.shapeFileToMATSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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

import playground.gregor.shapeFileToMATSim.GraphGenerator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class PolygonGeneratorII {

	private QuadTree<Polygon> polygonTree;
	private QuadTree<Feature> lineTree;
	private HashSet<Polygon> polygons;
	private HashMap<Integer, LineString> lineStrings;
	private HashMap<Integer, Polygon> returnPolys;
	private HashSet<Polygon> cutPolys = new HashSet<Polygon>(); 
	private FeatureSource featureSourcePolygon;
	private FeatureSource featureSourceLineString;
	private FeatureCollection collectionLineString;
	private List<Feature> featureList;
	private GeometryFactory geofac;
	static final double CATCH_RADIUS = 0.1;
	static final double DEFAULT_DISTANCE = 10;
	private static final Logger log = Logger.getLogger(PolygonGeneratorII.class);
	private boolean graph = false; 
	
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
		mergePolygons();
		cutPolygons();
		log.info("leaving polygonGenerator");
		
		return(genPolygonFeatureCollection());			
		
	}
	
	private void cutPolygons(){
		
		log.info("cutting polygons ...");
		
		 
		HashSet<Point> currPoints = new HashSet<Point>();
		HashSet<Point> donePoints = new HashSet<Point>();
		currPoints.add(lineStrings.get(0).getEndPoint());
				
		
		while(!currPoints.isEmpty()){
			
			HashSet<Point> nextPoints = new HashSet<Point>();
		
			for(Iterator<Point> iter = currPoints.iterator() ; iter.hasNext() ; ){
			
				Point currPoint = iter.next();
				Coordinate currPointCoor = currPoint.getCoordinate();
				List<Feature> lines = (List<Feature>) lineTree.get(currPoint.getX(), currPoint.getY(), CATCH_RADIUS);
				if(lines.size()<3){ 
					continue;
				}
				List<Polygon> nodePolys = new ArrayList<Polygon>();								
				
				for(Feature ft : lines ){
					
					LineString ls = (LineString) ft.getAttribute(0);
					if(returnPolys.containsKey(ft.getAttribute(1))){
						nodePolys.add(returnPolys.get(ft.getAttribute(1)));
					}					
					if((ls.getStartPoint().equalsExact(currPoint, CATCH_RADIUS))&& (donePoints.contains(ls.getEndPoint()) == false)){
						nextPoints.add(ls.getEndPoint());
						
					}else if(donePoints.contains(ls.getStartPoint()) == false){
						nextPoints.add(ls.getStartPoint());
					}
				}
								
				Geometry[] geoArr = new Geometry[0];
				GeometryCollection geoColl = new GeometryCollection(nodePolys.toArray(geoArr),geofac);
				Geometry poly = (Geometry) geoColl.buffer(0.0);
				
				Coordinate [] coor = poly.getCoordinates();
				
				Envelope o = poly.getEnvelopeInternal();
				
				List<Point> points = new ArrayList<Point>();				
										
				SortedMap<Double, LineString> sortedLines = sortLines(lines,currPoint);
				LineString [] l = new LineString[0];
				Double [] d = new Double[0];
				
				LineString [] lineArr = sortedLines.values().toArray(l);
				Double [] angleArr = sortedLines.keySet().toArray(d);
				
				for (int i = 0 ; i < lineArr.length ; i++ ){
					
					LineString lineI = lineArr[i];
					double angleI = angleArr[i];
					LineString lineII;
					double angleII;
					double deltaAngle;
					if (i == ( lineArr.length -1) ) {	
						lineII = lineArr[0];
						angleII = angleArr[0];
						deltaAngle = 360 - (angleII - angleI);			
					}else {
						lineII = lineArr[i+1];
						angleII = angleArr[i+1];
						deltaAngle = angleII - angleI;
					}
					
					Coordinate bisecCoor = getBisectingLine(lineI, lineII);
					
					if (deltaAngle > 180 ){
						Coordinate co = subCoord(currPointCoor, bisecCoor); 
						bisecCoor = new Coordinate(currPointCoor.x + co.x , currPointCoor.y + co.y);
					}
											
					Coordinate [] quadcoor = new Coordinate [] {lineI.getStartPoint().getCoordinate(), lineI.getEndPoint().getCoordinate(),
							bisecCoor, lineII.getEndPoint().getCoordinate(), lineII.getStartPoint().getCoordinate()}; 
					CoordinateSequence triseq = new CoordinateArraySequence(quadcoor);
					LinearRing quadRing = new LinearRing(triseq, geofac);
					Polygon pol = new Polygon(quadRing, null, geofac);
					
					boolean found = false;
//					List<Coordinate> cList = new ArrayList<Coordinate>();
					QuadTree<Coordinate> polyCoor = new QuadTree<Coordinate>(o.getMinX(), o.getMinY(), o.getMaxX(), o.getMaxY());
					
					for(int ii = 0 ; ii < coor.length ; ii++ ){
						polyCoor.put(coor[ii].x, coor[ii].y, coor[ii]);
					}
					
					while(!found){
						
						Coordinate pp = polyCoor.get(currPointCoor.x, currPointCoor.y);
						
						Coordinate [] cc = new Coordinate[]{currPointCoor, pp};
						CoordinateSequence seq = new CoordinateArraySequence(cc);
						LineString line = new LineString(seq, geofac);
						
						if(line.crosses(pol)|| pol.contains(line)){
							
							Coordinate [] p = new Coordinate[]{pp};
							CoordinateSequence seqII = new CoordinateArraySequence(p);
							Point po = new Point(seqII, geofac);
							points.add(po);
							
							found = true;
							break;
						}else{
							polyCoor.remove(pp.x, pp.y, pp);
						}
					}	
					
				}
				Coordinate [] coords = coor;
				for(Point p : points){
					log.info(p.getCoordinate().toString());
				}
				
				for(int it1 = 0 ; it1 < points.size() ; it1++){        
					
					List<Coordinate> newPoly = new ArrayList<Coordinate>();
					List<Coordinate> restPoly = new ArrayList<Coordinate>();
					
					int countPoints = 0;
				
					for (int i = 0 ; i < coords.length ; i++){
						CoordinateSequence seq = new CoordinateArraySequence(new Coordinate []{coords[i]});
						Point p = new Point(seq, geofac);
					
						if (countPoints == 0 || countPoints == points.size()){
							
							newPoly.add(coords[i]);				
							for (Iterator<Point> it = points.iterator() ; it.hasNext() ; ){
								Point p1 = (Point) it.next();
								if ((p.equalsExact(p1, CATCH_RADIUS)) && (i != 0)){
									restPoly.add(coords[i]);						
									countPoints++;
									break;
								}
							}
						}else{
							restPoly.add(coords[i]);
							for (Iterator<Point> it = points.iterator() ; it.hasNext() ; ){
								Point p1 = (Point) it.next();
								
								if ((countPoints == (points.size()-1)) && (p.equalsExact(p1, CATCH_RADIUS))){
									newPoly.add(coords[i]);
									countPoints++;
									break;
								}else if (p.equalsExact(p1, CATCH_RADIUS)){
									countPoints++;
								}
							}
						}
					}
					if (newPoly.get(0) != newPoly.get(newPoly.size()-1)){
						newPoly.add(newPoly.get(0));
					}
					if ((!restPoly.isEmpty()) && restPoly.get(0) != restPoly.get(restPoly.size()-1)){
						restPoly.add(restPoly.get(0));
					}
					
					Coordinate [] co = new Coordinate[0];
					
					Coordinate [] coNew = newPoly.toArray(co);
					CoordinateSequence seq = new CoordinateArraySequence(coNew);
					try{LinearRing lr = new LinearRing(seq, geofac);
					Polygon p = new Polygon(lr, null, geofac);
					cutPolys.add(p);
					}catch(Exception e){
						log.info(e);
					}
					
					
					coords = restPoly.toArray(co);
				}
				
				points.add(points.get(0));
				List<Coordinate> pcoor = new ArrayList<Coordinate>();
				
				for(Point p : points){
					pcoor.add(p.getCoordinate());
				}
				Coordinate [] cos = new Coordinate[0];
				Coordinate [] cosNew = pcoor.toArray(cos);
 				CoordinateSequence seq = new CoordinateArraySequence(cosNew);
				LinearRing lr = new LinearRing(seq, geofac);
				Polygon p = new Polygon(lr, null, geofac);
				cutPolys.add(p);
						
			}
			donePoints.addAll(currPoints);
			currPoints.clear();
			currPoints.addAll(nextPoints);			
		}
		
		if(cutPolys.isEmpty()){log.info("no Poly");}
		log.info("done.");
	}
		
	private void mergePolygons(){
		
		log.info("merging polygons ...");
		returnPolys = new HashMap<Integer, Polygon>();
		
		for (Iterator lsIt = lineStrings.keySet().iterator() ; lsIt.hasNext() ; ){
		
			Integer id = (Integer) lsIt.next();
						
			LineString ls = lineStrings.get(id);
			
			Collection<Polygon> polys = new ArrayList<Polygon>();
			List<Polygon> finalPolys = new ArrayList<Polygon>();
			polys = polygonTree.get(ls.getCentroid().getX(),ls.getCentroid().getY() , 500);
//			polygonTree.get(c[0].x-CATCH_RADIUS, c[0].y-CATCH_RADIUS, c[2].x+CATCH_RADIUS, c[2].y+CATCH_RADIUS, polys);
			Coordinate [] linkCoord = ls.getCoordinates();	
//			GeometryFactory geofac = new GeometryFactory();
							
			for(int i=0 ; i < linkCoord.length ; i++){												
				
				Coordinate [] g = new Coordinate[]{};	
				CoordinateSequence seq = new CoordinateArraySequence(g);
        		Point point = new Point(seq,geofac);
				
        		for (Polygon po : polys){
					if(po.covers(point)||po.crosses(ls)){
						finalPolys.add(po);
					}	
				}			
			}
			
			if(finalPolys.isEmpty()){	continue;}
   			   			   			
			Geometry [] gA = new Geometry[0];
   			Geometry [] geoArray = finalPolys.toArray(gA);
   			GeometryCollection geoColl = new GeometryCollection(geoArray,geofac);	
   			   			 			
   			try{
   			Geometry retPoly = (Geometry) geoColl.buffer(0.0);
   			
   			for (int i = 0; i < retPoly.getNumGeometries(); i++) {
				Polygon polygon = (Polygon) retPoly.getGeometryN(i);
				if(!polygon.isEmpty()){					
					returnPolys.put( id ,polygon);
				}
			}
   			
   			}catch(Exception e){
   				e.printStackTrace();
   			}
   			 
//   			double width = calcWitdh(ls,finalPolys);
		}

		log.info("done.");
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
	
	private Collection<Feature> genPolygonFeatureCollection() throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType width = AttributeTypeFactory.newAttributeType("width", Double.class);
		AttributeType area = AttributeTypeFactory.newAttributeType("area", Double.class);		
		FeatureType ftRoadShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id, width, area }, "linkShape");
		
		for (Iterator<Polygon> it = this.cutPolys.iterator() ; it.hasNext() ; ){	
			Feature ft = ftRoadShape.create(new Object [] {new MultiPolygon(new Polygon []{(Polygon)it.next()},this.geofac), -1, 0.0, 0.0},"network");
			features.add(ft);
		}
				
//		for (Iterator<Entry<Integer, Polygon>> it = this.returnPolys.entrySet().iterator() ; it.hasNext() ; ){	
//			Polygon poly = (Polygon)it.next().getValue();
//			Feature ft = ftRoadShape.create(new Object [] {new MultiPolygon(new Polygon []{ poly  },this.geofac), -1, 0.0, 0.0},"network");
//			features.add(ft);
//		}
		return features;
	}
	
	private Feature genLineStringFeature(Integer iid, LineString ls)throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		
		FeatureType ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id }, "linString");
		Feature ft = ftLineString.create(new Object [] {ls, iid},"lineString");
		return ft;
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
	
	////Only works for lineStrings with one segment
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
		if(start){
			CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(lineIcoor[0]), new Coordinate(lineIcoor[1])});
			System.out.println(lineIcoor[0].toString()+lineIcoor[1].toString());
			vec = new LineString(seqd, geofac);
		} else {
			int length = lineIcoor.length;
			CoordinateSequence seqd = new CoordinateArraySequence(new Coordinate[]{new Coordinate(lineIcoor[length-1]), new Coordinate(lineIcoor[length-2])});
			System.out.println(lineIcoor[length-1].toString()+lineIcoor[length-2].toString());
			vec = new LineString(seqd, geofac);		
		}
		return vec;
	}
		
//	private LineString getBisectingLine(LineString ls, double angle){
//		
//		Coordinate [] cI = ls.getCoordinates();
//		Coordinate coordI = subCoord(cI[1],cI[0]);
//		
//		Coordinate cII = multiCoord(coordI,1/(getLength(coordI) * DEFAULT_DISTANCE * Math.cos(angle))) ; 
//		Coordinate [] line = new Coordinate[]{cI[0],cII};
//		CoordinateSequence seqLine = new CoordinateArraySequence(line);
//		LineString rl = new LineString(seqLine,geofac);
//		return rl;
//	}
	
//	LineStrings should have two coordinates. The first should be equal in ls1 and ls2
	
	private Coordinate getBisectingLine(LineString ls1, LineString ls2){
		
		Coordinate c1 = subCoord(ls1.getCoordinates()[1],ls1.getCoordinates()[0]);
		Coordinate c2 = subCoord(ls2.getCoordinates()[1],ls2.getCoordinates()[0]);
		c1 = multiCoord(c1,(1/getLength(c1)));
		c2 = multiCoord(c2,(1/getLength(c2)));
		Coordinate r = multiCoord(subCoord(c2,c1),0.5);
		Coordinate r2 = new Coordinate((c1.x+r.x), (c1.y+r.y));
		return r2;
				
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
				sortedLines.put(360 - getAngle(li,yLine), line);
			}else{
				sortedLines.put(getAngle(li,yLine), line);
			}			
		}
		return sortedLines;
	}
}
