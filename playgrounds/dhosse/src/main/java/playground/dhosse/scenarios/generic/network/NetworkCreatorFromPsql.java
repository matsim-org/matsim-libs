package playground.dhosse.scenarios.generic.network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dhosse.scenarios.generic.Configuration;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * This class provides functionalities to create and modify a {@link org.matsim.api.core.v01.network.Network}
 * by using OpenStreetMap data stored in a postgreSQL database.
 * 
 * @author dhosse
 *
 */
public class NetworkCreatorFromPsql {
	
	private static final Logger log = Logger.getLogger(NetworkCreatorFromPsql.class);

	//MEMBERS
	private static final String TAG_ACCESS = "access";
	private static final String TAG_GEOMETRY = "st_astext";
	private static final String TAG_HIGHWAY = "highway";
	private static final String TAG_ID = "osm_id";
	private static final String TAG_JUNCTION = "junction";
//	private static final String TAG_LANES = "lanes"; TODO
//	private static final String TAG_MAXSPEED = "maxspeed"; TODO
	private static final String TAG_ONEWAY = "oneway";

	private static final String MOTORWAY = "motorway";
	private static final String MOTORWAY_LINK = "motorway_link";
	private static final String TRUNK = "trunk";
	private static final String TRUNK_LINK = "trunk_link";
	private static final String PRIMARY = "primary";
	private static final String PRIMARY_LINK = "primary_link";
	private static final String SECONDARY = "secondary";
	private static final String TERTIARY = "tertiary";
	private static final String MINOR = "minor";
	private static final String UNCLASSIFIED = "unclassified";
	private static final String RESIDENTIAL = "residential";
	private static final String LIVING_STREET = "living_street";
	
	private static int nodeCounter = 0;
	private static int linkCounter = 0;

	private final Scenario scenario;
	private final CoordinateTransformation transform;
	private final Configuration configuration;
	private final boolean scaleMaxSpeed;
	private final boolean cleanNetwork;
	private final boolean simplifyNetworK;
	
	//TODO what can you modify?
	static enum modification{};

	public NetworkCreatorFromPsql(final Scenario scenario, Configuration configuration, boolean scaleMaxSpeed){
		
		this(scenario, configuration, scaleMaxSpeed, false, false);
		
	};
	
	public NetworkCreatorFromPsql(final Scenario scenario, Configuration configuration, boolean scaleMaxSpeed, boolean cleanNetwork){
		
		this(scenario, configuration, scaleMaxSpeed, cleanNetwork, false);
		
	}
	
	public NetworkCreatorFromPsql(final Scenario scenario, Configuration configuration, boolean scaleMaxSpeed, boolean cleanNetwork,
			boolean simplifyNetwork){
		
		this.scenario = scenario;
		this.transform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, configuration.getCrs());
		this.configuration = configuration;
		this.scaleMaxSpeed = scaleMaxSpeed;
		this.cleanNetwork = cleanNetwork;
		this.simplifyNetworK = simplifyNetwork;
		
	}
	
	
	
	private Map<String, HighwayDefaults> highwayDefaults = new HashMap<String, HighwayDefaults>();
	
	public void create(){
		
		if(this.highwayDefaults.size() < 1){
			
			this.setHighwayDefaults(MOTORWAY, 1.0, 120/3.6, 1.0, 2000.0, true);
			this.setHighwayDefaults(MOTORWAY_LINK, 1,  80.0/3.6, 1.0, 1500, true);
			this.setHighwayDefaults(TRUNK, 1,  80.0/3.6, 1.0, 2000);
			this.setHighwayDefaults(TRUNK_LINK, 1,  50.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(PRIMARY, 1,  80.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(PRIMARY_LINK, 1,  60.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(SECONDARY, 1,  60.0/3.6, 1.0, 1000);
			this.setHighwayDefaults(TERTIARY, 1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(MINOR, 1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(UNCLASSIFIED, 1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(RESIDENTIAL, 1,  30.0/3.6, 1.0,  600);
			this.setHighwayDefaults(LIVING_STREET, 1,  15.0/3.6, 1.0,  300);
			
		}
		
		WKTReader wktReader = new WKTReader();
		Set<WayEntry> wayEntries = new HashSet<>();
		
		try {
			
			log.info("Connection to mobility database...");
		
			Class.forName("org.postgresql.Driver").newInstance();
			
			Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/geodata",
					configuration.getDatabaseUsername(), configuration.getPassword());
		
			if(connection != null){
				
				log.info("Connection establised.");
	
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("select osm_id, access, highway, junction, oneway,"
						+ " st_astext(way) from osm.osm_line;");
				
				while(result.next()){
					
					WayEntry entry = new WayEntry();
					entry.osmId = result.getString(TAG_ID);
					entry.accessTag = result.getString(TAG_ACCESS);
					entry.highwayTag = result.getString(TAG_HIGHWAY);
					entry.junctionTag = result.getString(TAG_JUNCTION);
//					entry.lanesTag = result.getString(TAG_LANES);
//					entry.maxspeedTag = result.getString(TAG_MAXSPEED);
					entry.onewayTag = result.getString(TAG_ONEWAY);
					entry.geometry = wktReader.read(result.getString(TAG_GEOMETRY));
					wayEntries.add(entry);
					
				}
				
				result.close();
				statement.close();
				
			}
			
			connection.close();
			
			processWayEntries(wayEntries);
			
			if(this.simplifyNetworK){
				
				new NetworkSimplifier().run(scenario.getNetwork());
				
			}
			
			if(this.cleanNetwork){
			
				new NetworkCleaner().run(scenario.getNetwork());
				
			}
			
			new NetworkWriter(scenario.getNetwork()).write(configuration.getWorkingDirectory() + "network_cleaned.xml.gz");
			
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException | ParseException e) {

			e.printStackTrace();
			
		}
		
	}
	
	public void setHighwayDefaults(final String highwayType, final double lanesPerDirection, final double freespeed,
			final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway){
		
		this.highwayDefaults.put(highwayType, new HighwayDefaults(freespeed, freespeedFactor, lanesPerDirection, laneCapacity_vehPerHour, oneway));
		
	}
	
	public void setHighwayDefaults(final String highwayType, final double lanesPerDirection, final double freespeed,
			final double freespeedFactor, final double laneCapacity_vehPerHour){
		
		this.setHighwayDefaults(highwayType, lanesPerDirection, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
		
	}
	
	/**
	 * 
	 * For later changes (e.g. additional modes, additional links)
	 * 
	 * @param network
	 */
	public void modify(Network network){
		
	}
	
	private void processWayEntries(Set<WayEntry> wayEntries){
		
		for(WayEntry entry : wayEntries){

			//if access is restricted, we skip the way
			if("no".equals(entry.accessTag)) continue;
			
			Coordinate[] coordinates = entry.geometry.getCoordinates();
			
			//calc length of the way
			if(coordinates.length > 1){
				
				Coordinate from = coordinates[0];
//				Coordinate to = coordinates[coordinates.length - 1];
				double length = 0.;
				Coordinate lastTo = from;
				
				for(int i = 1; i < coordinates.length; i++){
					
					Coordinate next = coordinates[i];
					
					length = CoordUtils.calcEuclideanDistance(MGC.coordinate2Coord(lastTo), MGC.coordinate2Coord(next));
					createLink(entry, length, lastTo, next);
					
					lastTo = next;
					
				}
				
			}
			
		}
		
		log.info("Conversion statistics:");
		log.info("OSM ways:     " + wayEntries.size());
		log.info("MATSim nodes: " + scenario.getNetwork().getNodes().size());
		log.info("MATSim links: " + scenario.getNetwork().getLinks().size());
		
	}
	
	private void createLink(WayEntry entry, double length, Coordinate from, Coordinate to){
		
		HighwayDefaults defaults = this.highwayDefaults.get(entry.highwayTag);
		
		//if there are defaults for the highway type of the current way, we proceed
		//else the way is simply skipped
		if(defaults != null){
			
			//set all values to default
			double freespeed = defaults.freespeed;
			double freespeedFactor = defaults.freespeedFactor;
			double lanesPerDirection = defaults.lanesPerDirection;
			double laneCapacity = defaults.laneCapacity;
			boolean oneway = defaults.oneway;
			boolean onewayReverse = false;

			//freespeed tag
			String freespeedTag = entry.maxspeedTag;
			if(freespeedTag != null){
				
				try{
					
					freespeed = Double.parseDouble(freespeedTag) / 3.6;
					
				} catch(NumberFormatException e){
					
					e.printStackTrace();
					
				}
				
			}
			
			if("roundabout".equals(entry.junctionTag)){
				
				oneway = true;
				
			}
			
			//oneway tag
			if(entry.onewayTag != null){

				if(entry.onewayTag.equals("yes") || entry.onewayTag.equals("true") || entry.onewayTag.equals("1")){
					
					oneway = true;
					
				} else if(entry.onewayTag.equals("no")){
					
					oneway = false;
					
				} else if(entry.onewayTag.equals("-1")){
					
					onewayReverse = true;
					oneway = false;
					
				}
				
			}
			
			if(entry.highwayTag.equalsIgnoreCase("trunk") || entry.highwayTag.equalsIgnoreCase("primary") ||
					entry.highwayTag.equalsIgnoreCase("secondary")){
	            
				if((oneway || onewayReverse) && lanesPerDirection == 1.0){
	            
					lanesPerDirection = 2.0;
					
	            }
				
			}
			
			String lanesTag = entry.lanesTag;
			if(lanesTag != null){
				
				try {
					double totalNofLanes = Double.parseDouble(lanesTag);
					if (totalNofLanes > 0) {
						lanesPerDirection = totalNofLanes;

			            if (!oneway && !onewayReverse) {
			            	
			                lanesPerDirection /= 2.;
			                
			            }
					}
				
				} catch (Exception e) {
					
					e.printStackTrace();
				
				}
				
			}
			
			double capacity = lanesPerDirection * laneCapacity;
			
			if(this.scaleMaxSpeed){
				
				freespeed *= freespeedFactor;
				
			}

			Coord fromCoord = this.transform.transform(MGC.coordinate2Coord(from));
			Node closestFromNode = ((NetworkImpl)scenario.getNetwork()).getNearestNode(fromCoord);
			Node fromNode = setNode(fromCoord, closestFromNode);
			
			Coord toCoord = this.transform.transform(MGC.coordinate2Coord(to));
			Node closestToNode = ((NetworkImpl)scenario.getNetwork()).getNearestNode(toCoord);
			
			Node toNode = setNode(toCoord, closestToNode);
			
			String origId = entry.osmId;
			
			if(!onewayReverse){
				
				Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(linkCounter), fromNode, toNode);
				link.setCapacity(capacity);
				link.setFreespeed(freespeed);
				link.setLength(length);
				link.setNumberOfLanes(lanesPerDirection);
				
				if(link instanceof LinkImpl){
					
					((LinkImpl)link).setOrigId(origId);
					((LinkImpl)link).setType(entry.highwayTag);
					
				}
				
				scenario.getNetwork().addLink(link);
				linkCounter++;
				
			}
			
			if(!oneway){
				
				Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(linkCounter), toNode, fromNode);
				link.setCapacity(capacity);
				link.setFreespeed(freespeed);
				link.setLength(length);
				link.setNumberOfLanes(lanesPerDirection);
				
				if(link instanceof LinkImpl){
					
					((LinkImpl)link).setOrigId(origId);
					((LinkImpl)link).setType(entry.highwayTag);
					
				}
				
				scenario.getNetwork().addLink(link);
				linkCounter++;
				
			}
			
		}
		
	}

	//basically a copy from OsmNetworkReader
//	private void createLink(WayEntry entry, double length){
//	
//		HighwayDefaults defaults = this.highwayDefaults.get(entry.highwayTag);
//		
//		//if there are defaults for the highway type of the current way, we proceed
//		//else the way is simply skipped
//		if(defaults != null){
//			
//			//set all values to default
//			double freespeed = defaults.freespeed;
//			double freespeedFactor = defaults.freespeedFactor;
//			double lanesPerDirection = defaults.lanesPerDirection;
//			double laneCapacity = defaults.laneCapacity;
//			boolean oneway = defaults.oneway;
//			boolean onewayReverse = false;
//
//			//freespeed tag
//			String freespeedTag = entry.maxspeedTag;
//			if(freespeedTag != null){
//				
//				try{
//					
//					freespeed = Double.parseDouble(freespeedTag) / 3.6;
//					
//				} catch(NumberFormatException e){
//					
//					e.printStackTrace();
//					
//				}
//				
//			}
//			
//			if("roundabout".equals(entry.junctionTag)){
//				
//				oneway = true;
//				
//			}
//			
//			//oneway tag
//			if(entry.onewayTag != null){
//
//				if(entry.onewayTag.equals("yes") || entry.onewayTag.equals("true") || entry.onewayTag.equals("1")){
//					
//					oneway = true;
//					
//				} else if(entry.onewayTag.equals("no")){
//					
//					oneway = false;
//					
//				} else if(entry.onewayTag.equals("-1")){
//					
//					onewayReverse = true;
//					oneway = false;
//					
//				}
//				
//			}
//			
//			if(entry.highwayTag.equalsIgnoreCase("trunk") || entry.highwayTag.equalsIgnoreCase("primary") ||
//					entry.highwayTag.equalsIgnoreCase("secondary")){
//	            
//				if((oneway || onewayReverse) && lanesPerDirection == 1.0){
//	            
//					lanesPerDirection = 2.0;
//					
//	            }
//				
//			}
//			
//			String lanesTag = entry.lanesTag;
//			if(lanesTag != null){
//				
//				try {
//					double totalNofLanes = Double.parseDouble(lanesTag);
//					if (totalNofLanes > 0) {
//						lanesPerDirection = totalNofLanes;
//
//			            if (!oneway && !onewayReverse) {
//			            	
//			                lanesPerDirection /= 2.;
//			                
//			            }
//					}
//				
//				} catch (Exception e) {
//					
//					e.printStackTrace();
//				
//				}
//				
//			}
//			
//			double capacity = lanesPerDirection * laneCapacity;
//			
//			if(this.scaleMaxSpeed){
//				
//				freespeed *= freespeedFactor;
//				
//			}
//
//			Coord from = this.transform.transform(MGC.coordinate2Coord(entry.geometry.getCoordinates()[0]));
//			Node closestFromNode = ((NetworkImpl)scenario.getNetwork()).getNearestNode(from);
//			Node fromNode = setNode(from, closestFromNode);
//			
//			Coord to = this.transform.transform(MGC.coordinate2Coord(entry.geometry.getCoordinates()[entry.geometry.getCoordinates().length - 1]));
//			Node closestToNode = ((NetworkImpl)scenario.getNetwork()).getNearestNode(to);
//			
//			Node toNode = setNode(to, closestToNode);
//			
//			String origId = entry.osmId;
//			
//			if(!onewayReverse){
//				
//				Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(linkCounter), fromNode, toNode);
//				link.setCapacity(capacity);
//				link.setFreespeed(freespeed);
//				link.setLength(length);
//				link.setNumberOfLanes(lanesPerDirection);
//				
//				if(link instanceof LinkImpl){
//					
//					((LinkImpl)link).setOrigId(origId);
//					((LinkImpl)link).setType(entry.highwayTag);
//					
//				}
//				
//				scenario.getNetwork().addLink(link);
//				linkCounter++;
//				
//			}
//			
//			if(!oneway){
//				
//				Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(linkCounter), toNode, fromNode);
//				link.setCapacity(capacity);
//				link.setFreespeed(freespeed);
//				link.setLength(length);
//				link.setNumberOfLanes(lanesPerDirection);
//				
//				if(link instanceof LinkImpl){
//					
//					((LinkImpl)link).setOrigId(origId);
//					((LinkImpl)link).setType(entry.highwayTag);
//					
//				}
//				
//				scenario.getNetwork().addLink(link);
//				linkCounter++;
//				
//			}
//			
//		}
//		
//	}
	
	private Node setNode(Coord coord, Node closestNode){
		
		Node node = null;
		
		if(closestNode != null){
		
			if(CoordUtils.calcEuclideanDistance(coord, closestNode.getCoord()) == 0){
				
				node = closestNode;
				
				
			} else {
				node = scenario.getNetwork().getFactory().createNode(Id.createNodeId(nodeCounter), coord);
				scenario.getNetwork().addNode(node);
				nodeCounter++;
			}
			
		} else {
			node = scenario.getNetwork().getFactory().createNode(Id.createNodeId(nodeCounter), coord);
			scenario.getNetwork().addNode(node);
			nodeCounter++;
		}
		
		return node;
		
	}

	static class HighwayDefaults{
		
		double freespeed;
		double freespeedFactor;
		double lanesPerDirection;
		double laneCapacity;
		boolean oneway;
		
		HighwayDefaults(double freespeed, double freespeedFactor, double lanesPerDirection, double laneCapacity, boolean oneway){
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.lanesPerDirection = lanesPerDirection;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
		
	}
	
	static class WayEntry{
		
		String osmId;
		
		String accessTag;
		String highwayTag;
		String junctionTag;
		String lanesTag;
		String maxspeedTag;
		String onewayTag;
		
		Geometry geometry;
		
	}
	
}
