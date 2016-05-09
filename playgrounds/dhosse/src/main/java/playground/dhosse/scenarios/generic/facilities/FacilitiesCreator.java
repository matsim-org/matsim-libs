package playground.dhosse.scenarios.generic.facilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

import playground.dhosse.scenarios.generic.Configuration;
import playground.dhosse.scenarios.generic.utils.ActivityTypes;
import playground.dhosse.utils.osm.OsmKey2ActivityType;
import playground.johannes.gsv.synPop.osm.OSMNode;
import playground.johannes.gsv.synPop.osm.OSMWay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class FacilitiesCreator {

	private static int facilitiesCounter = 0;
	
	private static GeometryFactory factory = new GeometryFactory();

	private static WKTReader wktReader = new WKTReader();
	
	private static final String AMENITY = "amenity";
	private static final String SHOP = "shop";
	private static final String TOURISM = "tourism";
	private static final String LEISURE = "leisure";
	
	public static void run(Configuration configuration, Scenario scenario){
		
		generateFacilities(scenario, configuration);
		
	}
	
	static void generateFacilities(final Scenario scenario, Configuration configuration){
		
		ActivityFacilitiesFactoryImpl ffactory = new ActivityFacilitiesFactoryImpl();
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		
		try {

			Class.forName("org.postgresql.Driver").newInstance();
			
			Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/geodata",
					configuration.getDatabaseUsername(), configuration.getPassword());
		
			if(connection != null){
				
				createAndAddPointGeometries(connection, scenario, ffactory, facilities);
				createAndAddLineGeometries(connection, scenario, ffactory, facilities);
				createAndAddPolygonGeometries(connection, scenario, ffactory, facilities);
				
			}
			
			connection.close();
			
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException | ParseException e) {
			
			e.printStackTrace();
			
		}
		
//		GeometryUtils.writeFacilities2Shapefile(facilities.getFacilities().values(), outputShapefile, toCrs);
		
	}
	
	private static void createAndAddPointGeometries(Connection connection, Scenario scenario,
			ActivityFacilitiesFactoryImpl factory, ActivityFacilities facilities)
					throws SQLException, ParseException{
		
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery("select amenity,leisure,shop,tourism,st_astext(point)"
				+ " from XXX_osm_point where amenity is not null or leisure is not null or shop is not null"
				+ " or tourism is not null;");
		
		while(result.next()){

			String amenityType = result.getString(AMENITY);
			String leisureType = result.getString(LEISURE);
			String shopType = result.getString(SHOP);
			String tourismType = result.getString(TOURISM);
			Point point = (Point)wktReader.read(result.getString("st_astext"));
			
			Coord coord = MGC.point2Coord(point);
			
			ActivityFacility facility = factory.createActivityFacility(
					Id.create(facilitiesCounter, ActivityFacility.class), MGC.point2Coord(point),
					NetworkUtils.getNearestLink(scenario.getNetwork(), coord).getId());
			
			Set<String> actTypes = getActivityOption(new String[]{amenityType,leisureType,shopType,tourismType});
			
			//we don't want any facilities with no activity options
			if(!actTypes.isEmpty()){

				for(String actType : actTypes){

					ActivityOption ao = factory.createActivityOption(actType);
					facility.addActivityOption(ao);
					
				}
				
				facilities.addActivityFacility(facility);
				
				facilitiesCounter++;
				
			}
			
		}
		
		result.close();
		statement.close();
		
	}
	
	private static void createAndAddLineGeometries(Connection connection, Scenario scenario,
			ActivityFacilitiesFactoryImpl factory, ActivityFacilities facilities) throws SQLException{
		
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery("select * from XXX_osm_line where amenity is not null"
				+ "or shop is not null or tourism is not null;");
		
		while(result.next()){
			//TODO create and add facilities
		}
		
		result.close();
		statement.close();
		
	}
	
	private static void createAndAddPolygonGeometries(Connection connection, Scenario scenario,
			ActivityFacilitiesFactoryImpl factory, ActivityFacilities facilities) throws SQLException{
		
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery("select * from XXX_osm_polygon where amenity is not null"
				+ "or shop is not null or tourism is not null;");
		
		while(result.next()){
			//TODO create and add facilities
		}
		
		result.close();
		statement.close();
		
	}
	
	private static Set<String> getActivityOption(String[] key){
		
		Set<String> options = new HashSet<>();
		
		if(OsmKey2ActivityType.groceryShops.contains(key)){
			
			options.add(ActivityTypes.SUPPLY);
			
		} else if(OsmKey2ActivityType.miscShops.contains(key)){
			
			options.add(ActivityTypes.SHOPPING);
			
		} else if(OsmKey2ActivityType.education.contains(key)){
			
			options.add(ActivityTypes.EDUCATION);
			
		} else if(OsmKey2ActivityType.otherPlaces.contains(key)){
			
			options.add(ActivityTypes.OTHER);
			
		} else if(OsmKey2ActivityType.leisure.contains(key)){
			
			options.add(ActivityTypes.LEISURE);
			
		}
		
		return options;
		
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
