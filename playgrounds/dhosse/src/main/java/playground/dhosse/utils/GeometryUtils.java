package playground.dhosse.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeometryUtils {
	
	private static final Logger log = Logger.getLogger(GeometryUtils.class);
	
	private static final String TAB = "\t";
	private static final String END = "END";
	
	static final String BIG_ENDIAN = "00";
	static final String LITTLE_ENDIAN = "01";
	static final String WKB_POINT = "00000001";
	static final String WKB_LINE_STRING = "00000002";
	static final String WKB_MULTI_LINE_STRING = "00000005";
	
	public static Geometry createGeometryFromPolygonFile(String file){
		
		Set<LinkedList<Coordinate>> additiveGeometriesSet = new HashSet<>();
		Set<LinkedList<Coordinate>> subtractiveGeometriesSet = new HashSet<>();
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String line = null;
		LinkedList<Coordinate> coordinateList = new LinkedList<>();
		String type = null;
		
		try {
			
			while((line = reader.readLine()) != null){

				String[] parts = line.split(TAB);
				
				if(parts[0].equals(END) || type == null){
					
					if(type == null){
					
						if(parts[0].startsWith("!")){
							
							type = "sub";
							
						} else{
							
							type = "add";
							
						}
						
					} else if(parts[0].equals(END)){

						if(type.equals("add")){

							additiveGeometriesSet.add(coordinateList);
							
						} else {
							
							subtractiveGeometriesSet.add(coordinateList);
							
						}
						
						coordinateList = new LinkedList<>();
						type = null;
						
					}
					
				} else{

					if(parts.length < 2){
						
						String[] subParts = parts[0].split("   ")[1].split(" ");
						coordinateList.addLast(new Coordinate(Double.parseDouble(subParts[0]), Double.parseDouble(subParts[1])));
						
					} else {
						
						coordinateList.addLast(new Coordinate(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
						
					}
					
				}
				
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		LinearRing[] holes = new LinearRing[subtractiveGeometriesSet.size()];

		List<Coordinate> shellCoordinatesList = new ArrayList<>();
		
		for(List<Coordinate> coordinatesList : additiveGeometriesSet){
			
			shellCoordinatesList.addAll(coordinatesList);
			
		}
		
		int index = 0;
		for(List<Coordinate> hole : subtractiveGeometriesSet){
			
			Coordinate[] coordinates = new Coordinate[hole.size()];
			holes[index] = new GeometryFactory().createLinearRing(hole.toArray(coordinates));
			index++;
			
		}
		
		Coordinate[] coordinates = new Coordinate[shellCoordinatesList.size()];

		Polygon p1 = new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(shellCoordinatesList.toArray(coordinates)), holes);
		
		return new GeometryFactory().createMultiPolygon(new Polygon[]{p1});
		
	}
	
	public static void writeNetwork2Shapefile(Network network, String shapefilePath, String crs){
		
		Collection<SimpleFeature> linkFeatures = new ArrayList<SimpleFeature>();
		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("type", String.class).
				addAttribute("capacity", Double.class).
				addAttribute("freespeed", Double.class).
				addAttribute("modes", String.class).
				addAttribute("nLanes", Integer.class).
				addAttribute("origId", String.class).
				create();

		for (Link link : network.getLinks().values()) {
			Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),
					link.getToNode().getId().toString(), link.getLength(), ((LinkImpl)link).getType(),
					link.getCapacity(), link.getFreespeed(), CollectionUtils.setToString(link.getAllowedModes()),
					link.getNumberOfLanes(), ((LinkImpl)link).getOrigId()}, null);
			linkFeatures.add(ft);
		}
		
		if(linkFeatures.size() > 0){
			
			ShapeFileWriter.writeGeometries(linkFeatures, shapefilePath + "/links.shp");
			
		} else {
			
			log.error("Link feature collection is empty and thus there is no file to write...");
			
		}
		
		Collection<SimpleFeature> nodeFeatures = new ArrayList<>();
		PointFeatureFactory pointFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				addAttribute("id", String.class).
				create();
		
		for(Node node : network.getNodes().values()){
			
			SimpleFeature feature = pointFactory.createPoint(MGC.coord2Coordinate(node.getCoord()),
					new Object[]{node.getId().toString()}, null);
			nodeFeatures.add(feature);
			
		}
		
		if(nodeFeatures.size() > 0){
			
			ShapeFileWriter.writeGeometries(nodeFeatures, shapefilePath + "/nodes.shp");
			
		} else {
			
			log.error("Point feature collection is empty and thus there is no file to write...");
			
		}
		
	}
	
	public static void writeFacilities2Shapefile(Collection<? extends ActivityFacility> facilities, String shapefile, String crs){
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("facilities").
				addAttribute("ID", String.class).
				addAttribute("actType", String.class).
				create();
				
		for(ActivityFacility facility : facilities){
			
			Set<String> options = new HashSet<>();
			options.addAll(facility.getActivityOptions().keySet());
			SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(facility.getCoord()),
					new Object[]{facility.getId().toString(), CollectionUtils.setToString(options)},
					null);
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, shapefile);
		
	}
	
	public static void writeStopFacilities2Shapefile(Collection<? extends TransitStopFacility> facilities, String shapefile, String crs){
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("stops").
				addAttribute("ID", String.class).
				addAttribute("name", String.class).
				addAttribute("linkId", String.class).
				create();
		
		for(TransitStopFacility facility : facilities){
			
			SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(facility.getCoord()),
					new Object[]{facility.getId().toString(), facility.getName(),
					facility.getLinkId().toString()},
					null);
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, shapefile + "/stops.shp");
		
	}
	
	public static String geometryToWKB(Geometry g){
		
		return geometryToWKB(g, TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:4326"));
		
	}
	
	/**
	 * 
	 * Converts the given geometry into a well-known binary string.
	 * 
	 * @param g the geometry
	 * @return the wkb string representation of the geometry
	 */
	public static String geometryToWKB(Geometry g, CoordinateTransformation transformation){
		
		StringBuilder wkb = new StringBuilder(BIG_ENDIAN);
		
		if(g instanceof Point){
			
			wkb.append(WKB_POINT);
			
		} else if(g instanceof LineString){
			
			wkb.append(WKB_LINE_STRING);
			
		} else if(g instanceof MultiLineString){
			
			wkb.append(WKB_MULTI_LINE_STRING);
			
		}
		
		for(Coordinate coordinate : g.getCoordinates()){
			
			Coord c = transformation.transform(MGC.coordinate2Coord(coordinate));
			
			wkb.append(Long.toHexString(Double.doubleToRawLongBits(c.getX())));
			wkb.append(Long.toHexString(Double.doubleToRawLongBits(c.getY())));
			
		}
		
		return wkb.toString();
		
	}
	
	public static Coord shoot(Geometry geometry){
		
		Point point = null;
		double x, y;
		
		do{
			
			x = geometry.getEnvelopeInternal().getMinX() + Global.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX());
	  	    y = geometry.getEnvelopeInternal().getMinY() + Global.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY());
	  	    point = MGC.xy2Point(x, y);
			
		}while(!geometry.contains(point));
		
		return MGC.point2Coord(point);
		
	}
	
}
