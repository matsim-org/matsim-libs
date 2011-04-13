package playground.florian.GTFSConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.*;

import com.vividsolutions.jts.geom.Coordinate;

public class GtfsConverter {

	private String filepath = "";
	
	private TransitScheduleFactory tf = new TransitScheduleFactoryImpl();
	
	public static void main(String[] args) {
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input");
		gtfs.convert(7);
	}
	
	public GtfsConverter(String filepath){
		this.filepath = filepath;
	}
	
	public void convert(int weekday){
		// 1 = monday, 2 = tuesday,...
		// Create Schedule
		TransitSchedule ts = tf.createTransitSchedule();
		// Put all stops in the Schedule
		this.convertStops(ts);
		// Get the Routenames and the assigned Trips
		Map<Id,String> routeNames = this.getRouteNames();
		Map<Id,Id> routeToTripAssignments = this.getRouteToTripAssignments();
		// Create Transitlines
		this.createTransitLines(ts, routeNames);
		// Get the used service Id for the choosen weekday
		List<String> usedServiceIds = this.getUsedServiceIds(weekday);
		System.out.println("Reading of ServiceIds succesfull: " + usedServiceIds);
		// Get the TripIds, which are available for the serviceIds 
		List<Id> usedTripIds = this.getUsedTripIds(usedServiceIds);
		System.out.println("Reading of TripIds succesfull: " + usedTripIds);
		// Build and safe a Network --- IT PROBABLY NEEDS A TRANSFORMATION
		NetworkImpl net = this.createNetworkOfStopsAndTrips(ts);
		new NetworkWriter(net).write("./network.xml");
		// Assign the links of the Network to the stops --- NOT WORKING YET
//		this.assignLinksToStops(ts, net);
		// Convert the schedules for the trips
		this.convertSchedules(ts, routeNames, routeToTripAssignments, usedTripIds, net);

		
		TransitScheduleWriter tsw = new TransitScheduleWriter(ts);
		try {
			tsw.writeFile("./transitSchedule.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private List<Id> getUsedTripIds(List<String> usedServiceIds) {
		String tripsFilename = filepath + "/trips.txt";
		List<Id> usedTripIds = new ArrayList<Id>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(tripsFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int serviceIdIndex = header.indexOf("service_id");
			int tripIdIndex = header.indexOf("trip_id");
			String row = br.readLine();
			do {
				String[] entries = row.split(",");
				if(usedServiceIds.contains(entries[serviceIdIndex])){
					usedTripIds.add(new IdImpl(entries[tripIdIndex]));
				}
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(tripsFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return usedTripIds;
	}
	
	private Map<Id,String> getRouteNames(){
		String routesFilename = filepath + "/routes.txt";
		Map<Id,String> routeNames = new HashMap<Id,String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(routesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int routeIdIndex = header.indexOf("route_id");
			int routeLongNameIndex = header.indexOf("route_long_name");
			String row = br.readLine();
			do {
				String[] entries = row.split(",");
				routeNames.put(new IdImpl(entries[routeIdIndex]), entries[routeLongNameIndex]);				
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(routesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return routeNames;
	}
	
	private Map<Id,Id> getRouteToTripAssignments(){
		String tripsFilename = filepath + "/trips.txt";
		Map<Id,Id> routeTripAssignment = new HashMap<Id,Id>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(tripsFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int routeIdIndex = header.indexOf("route_id");
			int tripIdIndex = header.indexOf("trip_id");
			String row = br.readLine();
			do {
				String[] entries = row.split(",");
				routeTripAssignment.put(new IdImpl(entries[tripIdIndex]), new IdImpl(entries[routeIdIndex]));				
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(tripsFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return routeTripAssignment;		
	}

	private List<String> getUsedServiceIds(int weekday){
		String calendarFilename = filepath + "/calendar.txt";
		List<String> serviceIds = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(calendarFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int serviceIdIndex = header.indexOf("service_id");
			int[] weekdayIndexes= new int[7];
			weekdayIndexes[0] = header.indexOf("monday");
			weekdayIndexes[1] = header.indexOf("tuesday");
			weekdayIndexes[2] = header.indexOf("wednesday");
			weekdayIndexes[3] = header.indexOf("thursday");
			weekdayIndexes[4] = header.indexOf("friday");
			weekdayIndexes[5] = header.indexOf("saturday");
			weekdayIndexes[6] = header.indexOf("sunday");
			String row = br.readLine();
			do {
				String[] entries = row.split(",");
				if(entries[weekdayIndexes[weekday-1]].equals("1")){
					serviceIds.add(entries[serviceIdIndex]);
				}
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(calendarFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serviceIds;
	}
	
	private void convertSchedules(TransitSchedule ts, Map<Id, String> routeNames, Map<Id, Id> routeToTripAssignments, List<Id> tripIds, NetworkImpl net){
		String stopTimesFilename = filepath + "/stop_times.txt";
		Map<String,TransitLine> transitLines = new HashMap<String, TransitLine>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int tripIdIndex = header.indexOf("trip_id");
			int arrivalTimeIndex = header.indexOf("arrival_time");
			int depatureTimeIndex = header.indexOf("depature_time");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			do {
				
				
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	private void convertStops(TransitSchedule ts){
		String filename = filepath + "/stops.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int stopIdIndex = header.indexOf("stop_id");
			int stopNameIndex = header.indexOf("stop_name");
			int stopLatitudeIndex = header.indexOf("stop_lat");
			int stopLongitudeIndex = header.indexOf("stop_lon");			
			String row = br.readLine();
			do{
				String[] entries = row.split(",");
				TransitStopFacility t = this.tf.createTransitStopFacility(new IdImpl(entries[stopIdIndex]), MGC.coordinate2Coord(new Coordinate(Double.parseDouble(entries[stopLongitudeIndex]), Double.parseDouble(entries[stopLatitudeIndex]))), false);
				t.setName(entries[stopNameIndex]);
				ts.addStopFacility(t);
				row = br.readLine();
			}while(row != null);
		} catch (FileNotFoundException e) {
			System.out.println(filename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void assignLinksToStops(TransitSchedule ts, NetworkImpl n){
		Map<Id,TransitStopFacility> stops = ts.getFacilities();
		for(TransitStopFacility stop: stops.values()){
			stop.setLinkId(n.getNearestLink(stop.getCoord()).getId());
		}
	}
	
	private void createTransitLines(TransitSchedule ts, Map<Id, String> routeNames) {
		for(Id id: routeNames.keySet()){
			TransitLine tl = tf.createTransitLine(id);
			ts.addTransitLine(tl);
		}		
	}

	private NetworkImpl createNetworkOfStopsAndTrips(TransitSchedule ts){
		// Standartvalues for links
		double freespeedKmPerHour=50; //km/h
		double capacity = 1500.0 / 3600.0;
		double freespeed = freespeedKmPerHour / 3.6;
		int numLanes = 1;
		long i = 0;
		// Create a new Network
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = sc.getNetwork();
		// Add all stops as nodes
		Map<Id, TransitStopFacility> stops = ts.getFacilities();
		for(Id id: stops.keySet()){
			TransitStopFacility stop = stops.get(id);
			NodeImpl n = new NodeImpl(id);
			n.setCoord(stop.getCoord());
			network.addNode(n);
		}
		// Get the Links from the trips in stop_times.txt
		String stopTimesFilename = filepath + "/stop_times.txt";
		Map<Id, Node> nodes = network.getNodes();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			do {
				String[] entries = row.split(",");
				Id fromNodeId = new IdImpl(entries[stopIdIndex]); // This works only as long as the nodes have the same ID as the stops;
				row = br.readLine();
				if(row!=null){
					entries = row.split(",");
					Id toNodeId = new IdImpl(entries[stopIdIndex]);
					double length = CoordUtils.calcDistance(nodes.get(fromNodeId).getCoord(), nodes.get(toNodeId).getCoord());
					network.createAndAddLink(new IdImpl(i++), nodes.get(fromNodeId), nodes.get(toNodeId), length, freespeed, capacity, numLanes);
					// Change the linktype to pt
					Link link = network.getLinks().get(new IdImpl(i-1));
					Set<String> modes = new HashSet<String>();
					modes.add(TransportMode.pt);
					link.setAllowedModes(modes);
					row = br.readLine();
				}		
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//(new NetworkCleaner()).run(network);
		return network;
	}

}
