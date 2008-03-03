package playground.gregor.shapeFileToMATSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class PolygonGeneratorII {

	private QuadTree<Polygon> polygonTree;
	private HashSet<Polygon> polygons;
	private HashSet<LineString> lineStrings;
	private HashSet<Polygon> returnPolys;
	private FeatureSource featureSourcePolygon;
	private FeatureSource featureSourceLineString;
	private FeatureCollection collectionLineString;
	private List<Feature> featureList;
	private GeometryFactory geofac;
	static final double CATCH_RADIUS = 100;
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
		
		return(genFeatureCollection());			
		
	}
	
	private void cutPolygons(){
		
		log.info("cutting polygons ...");
		
		
		
	
		log.info("done.");
	}
	
	private void mergePolygons(){
		
		log.info("merging polygons ...");
		returnPolys = new HashSet<Polygon>();
		
		for (LineString ls : this.lineStrings){
				
			Collection<Polygon> polys = new ArrayList<Polygon>();
			List<Polygon> finalPolys = new ArrayList<Polygon>();
			polys = polygonTree.get(ls.getCentroid().getX(),ls.getCentroid().getY() , 500);
//			polygonTree.get(c[0].x-CATCH_RADIUS, c[0].y-CATCH_RADIUS, c[2].x+CATCH_RADIUS, c[2].y+CATCH_RADIUS, polys);
			Coordinate [] linkCoord = ls.getCoordinates();	
			GeometryFactory geofac = new GeometryFactory();
							
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
					returnPolys.add(polygon);
				}
			}
   			
   			}catch(Exception e){
   				e.printStackTrace();
   			}
   			 
   			double width = calcWitdh(ls,finalPolys);
   			log.info(" " + width);
		}

		log.info("done.");
	}
		

	private void parsePolygons()throws Exception{
		
		log.info("parseing feaures ...");
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
		this.lineStrings = new HashSet<LineString>();
		while (iit.hasNext()) {
			Feature feature = (Feature) iit.next();
			MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				LineString lineString = (LineString) multiLineString.getGeometryN(i);
				lineStrings.add(lineString);
			}
		}
		
		log.info("done.");
	}

	private void add(Polygon po) {
		this.polygons.add(po);
		Point p = po.getCentroid();
		this.polygonTree.put(p.getX(), p.getY(), po);
	}	
	
	private Collection<Feature> genFeatureCollection() throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType width = AttributeTypeFactory.newAttributeType("width", Double.class);
		AttributeType area = AttributeTypeFactory.newAttributeType("area", Double.class);		
		FeatureType ftRoadShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id, width, area }, "linkShape");
		
		for (Polygon po : this.returnPolys){
			Feature ft = ftRoadShape.create(new Object [] {new MultiPolygon(new Polygon []{po},this.geofac), -1, 0.0, 0.0},"network");
			features.add(ft);
		}
		return features;
	}

//public Geometry computeUnionII (Geometry geometry){
//		
//		if (geometry instanceof GeometryCollection){
//				GeometryCollection collection = (GeometryCollection)geometry;
//				LinkedList<Geometry> glist = new LinkedList<Geometry>();
//				for (int i = 0; i < collection.getNumGeometries(); i += 1){
//					glist.add(computeUnionII(collection.getGeometryN(i)));
//				}
//				while (glist.size() > 1){
//					Geometry geom1 = glist.removeFirst();
//					Geometry geom2 = glist.removeFirst();
//					Geometry result = geom1.union(geom2);
//					if (result.getClass() == GeometryCollection.class){
//						Polygon [] poArr = new Polygon [result.getNumGeometries()]; 
//						for(int i = 0 ; i < result.getNumGeometries() ; i++){
//							Polygon poly = (Polygon) result.getGeometryN(i);
//							poArr[i] = poly;
//						}
//						MultiPolygon multiPoly = new MultiPolygon(poArr,this.geofac);
//						glist.addLast(multiPoly);
//					}else{
//						glist.addLast(result);
//					}
//				}
//				return (Geometry)glist.getFirst();
//		}else{
//			return geometry;
//		}
//	}
	
	private double calcWitdh(LineString ls, List<Polygon> po){
		
		
		List<Double> widths = new ArrayList<Double>();
		Coordinate [] linkCoord = ls.getCoordinates();	
									
		//Breiten ueber Linienpunkte 
		
		for(int i=0 ; i < linkCoord.length ; i++){												
			
			Coordinate [] g = new Coordinate[]{linkCoord[i]};
			
			CoordinateSequence seq = new CoordinateArraySequence(g);
    		Point point = new Point(seq,geofac);
			
    		Iterator<Polygon> polygonIterator = po.iterator();
    		
			
				while(polygonIterator.hasNext()){
						
						Polygon polygonGeo = polygonIterator.next();
						Coordinate coord1;
						Coordinate coord2;
						
						if(i==0){
							coord1 = new Coordinate(linkCoord[i]);
							coord2 = new Coordinate(linkCoord[i+1]);
						} else {
							coord1 = new Coordinate(linkCoord[i]);
							coord2 = new Coordinate(linkCoord[i-1]);
						}
						
						Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));
						
						Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
						Coordinate coordA = subCoord(coord1, multiCoord(ortho, 100));
						Coordinate coordB = subCoord(coord1, multiCoord(ortho, -100));
									
						Coordinate [] d = new Coordinate[]{coordA, coordB};
						CoordinateSequence seqd = new CoordinateArraySequence(d);
						LineString orthoLine = new LineString(seqd, geofac);			
														
						if(polygonGeo.intersects(orthoLine)){
							Coordinate [] inter = polygonGeo.intersection(orthoLine).getCoordinates();
							for(int iii = 0 ; iii < inter.length-1 ; iii++ ){
								widths.add(getLength(subCoord(inter[iii],inter[iii+1])));																			
							}
									
						}																				
					}
				}
			
		//Breiten ueber Polygonpunkte
		
		for(Iterator<Polygon> it = po.iterator() ; it.hasNext() ; ){
			
			Polygon polygonGeo = it.next();
			Coordinate [] polyCoord = polygonGeo.getCoordinates();

			for(int i=0 ; i<polyCoord.length-1 ; i++){																		
				
				Coordinate polyCoordX = polyCoord[i];
				
				for(int ii=0 ; ii<linkCoord.length-1 ; ii++){
					
					Coordinate coord1 = new Coordinate(linkCoord[ii]);
					Coordinate coord2 = new Coordinate(linkCoord[ii+1]);
												
					Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));
					Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
					
					Coordinate coordA = subCoord(polyCoordX, multiCoord(ortho, 100));
					Coordinate coordB = subCoord(polyCoordX, multiCoord(ortho, -100));
					
					Coordinate [] d = new Coordinate[]{coordA, coordB};
					CoordinateSequence seqd = new CoordinateArraySequence(d);
					LineString orthoLine = new LineString(seqd, geofac);
					
					Coordinate [] e = new Coordinate[]{coord1, coord2};
					CoordinateSequence seqe = new CoordinateArraySequence(e);
					LineString line = new LineString(seqe, geofac);
													
					if(polygonGeo.intersects(orthoLine) && orthoLine.intersects(line)){
						try{
							Coordinate [] inter = polygonGeo.intersection(orthoLine).getCoordinates();
							for(int iii = 0 ; iii < inter.length-1 ; iii++ ){
								widths.add(getLength(subCoord(inter[iii],inter[iii+1])));									
							}
						}catch(com.vividsolutions.jts.geom.TopologyException e1){
							log.info("TopologyException [Geometry.intersection()]: link:");
						}catch(Exception e1){
							log.info(e1);
						}
						
					}																					
				}
			}					
		}
		
		//Breite
		double minWidth = 100;
		for(Iterator<Double> it = widths.iterator() ; it.hasNext() ; ){					
			Double tmpWidth = (Double) it.next();
			double tmpWidthValue = tmpWidth.doubleValue();
			if(tmpWidthValue < minWidth)minWidth = tmpWidthValue;
		}
		
	return minWidth;	
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
		
}
