package playground.dhosse.scenarios.generic.facilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import playground.dhosse.scenarios.generic.utils.ActivityTypes;
import playground.dhosse.utils.osm.OsmKey2ActivityType;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.gsv.synPop.osm.OSMNode;
import playground.johannes.gsv.synPop.osm.OSMWay;
import playground.johannes.gsv.synPop.osm.XMLParser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class FacilitiesCreator {
	
	private static GeometryFactory factory = new GeometryFactory();
	
	public static void main(String args[]){

		if(args.length < 3){
			
			throw new RuntimeException("Too few arguments! Shutting down!");
			
		}
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		if(args.length == 4){
			
			generateFacilities(scenario, TransformationFactory.WGS84, args[0], args[1]);
			
		} else if(args.length == 5){
			
			generateFacilities(scenario, TransformationFactory.WGS84, args[0], args[1], args[4]);
			
		}
		
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[2]);
		new ObjectAttributesXmlWriter(scenario.getActivityFacilities().getFacilityAttributes()).writeFile(args[3]);
		
	}
	
	static void generateFacilities(final Scenario scenario, String fromCrs, String toCrs,
			String osmFile){
		
		generateFacilities(scenario, fromCrs, toCrs, osmFile, null);
		
	}
	
	static void generateFacilities(final Scenario scenario, String fromCrs, String toCrs,
			String osmFile, String outputShapefile){
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(osmFile);
		
		Collection<OSMNode> nodes = parser.getNodes().values();
		Collection<OSMWay> ways = parser.getWays().values();
		
		ActivityFacilitiesFactoryImpl ffactory = new ActivityFacilitiesFactoryImpl();
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		createFacilities(nodes, ways, ffactory, facilities, toCrs);
		
		if(outputShapefile != null){
			
		}
		
	}
	
	private static void createFacilities(Collection<OSMNode> nodes, Collection<OSMWay> ways,
			ActivityFacilitiesFactoryImpl factory, ActivityFacilities facilities, String toCrs){
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, toCrs);
		
		int cnt = 0;
		
		for(OSMNode node : nodes){
			
			Coord coord = ct.transform(new Coord(node.getLongitude(),node.getLatitude()));
			ActivityFacility facility = factory.createActivityFacility(Id.create(cnt, ActivityFacility.class), coord);
			Set<String> actTypes = getActivityOptions(node.tags());
			if(actTypes != null){
				
				cnt++;
				
				for(String actType : actTypes){
					
					ActivityOption option = factory.createActivityOption(actType);
					facility.addActivityOption(option);
					facilities.addActivityFacility(facility);
					
				}
				
			}
			
		}

		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, 
					CRSUtils.getCRS(Integer.parseInt(toCrs.replace("EPSG:", ""))));
		} catch (NumberFormatException | FactoryException e) {
			e.printStackTrace();
		}
		
		for (OSMWay way : ways) {
			
			Geometry geo = convert(way);
			
			if(geo != null){

				Point p = CRSUtils.transformPoint(geo.getCentroid(), transform);
				Coord coord = MatsimCoordUtils.pointToCoord(p);
				ActivityFacility facility = factory.createActivityFacility(
						Id.create(String.valueOf(cnt), ActivityFacility.class), coord);
				Set<String> actTypes = getActivityOptions(way.tags());
				if(actTypes != null){

					cnt++;
					
					for(String actType : actTypes){
						
						ActivityOption option = factory.createActivityOption(actType);
						facility.addActivityOption(option);
						facilities.addActivityFacility(facility);

					}
					
				}
				
			}
				
		}
		
	}
	
	private static Set<String> getActivityOptions(Map<String, String> tags){
		
		if(tags == null){
			return null;
		}
		
		Set<String> activityOptions = new HashSet<>();
		
		for(Entry<String, String> tag : tags.entrySet()){
			
			if(OsmKey2ActivityType.groceryShops.contains(tag.getValue())){
				
				activityOptions.add(ActivityTypes.SUPPLY);
				
			} else if(OsmKey2ActivityType.miscShops.contains(tag.getValue())){
				
				activityOptions.add(ActivityTypes.SHOPPING);
				
			} else if(OsmKey2ActivityType.education.contains(tag.getValue())){
				
				activityOptions.add(ActivityTypes.EDUCATION);
				
			} else if(OsmKey2ActivityType.otherPlaces.contains(tag.getValue())){
				
				activityOptions.add(ActivityTypes.OTHER);
				
			} else if(OsmKey2ActivityType.leisure.contains(tag.getValue())){
				
				activityOptions.add(ActivityTypes.LEISURE);
				
			}
			
		}
		
		if(activityOptions.isEmpty()){
			return null;
		} else {
			return activityOptions;
		}
		
	}
	
	private static Geometry convert(OSMWay way) {
		
		Coordinate[] coords = new Coordinate[way.getNodes().size()];
		
		for (int i = 0; i < way.getNodes().size(); i++) {
			OSMNode node = way.getNodes().get(i);
			coords[i] = new Coordinate(node.getLongitude(), node.getLatitude());
		}
		
		if(coords[0].x != coords[coords.length-1].x || coords[0].y != coords[coords.length-1].y){
			Coordinate[] tmp = coords.clone();
			coords = new Coordinate[tmp.length+1];
			for(int i = 0; i < tmp.length; i++){
				coords[i] = tmp[i];
			}
			coords[coords.length-1] = coords[0];
		}

		if(coords.length >= 0 && coords.length < 4){
			return null;
		}
		
		Polygon poly = factory.createPolygon(coords);

		return poly;
		
	}

}
