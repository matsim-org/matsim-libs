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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehiclesFactoryImpl;

public class GtfsConverter {

	private String filepath = "";
	
	
	
	private TransitScheduleFactory tf = new TransitScheduleFactoryImpl();
	
	private CoordinateTransformation transform = new GeotoolsTransformation("WGS84", "EPSG:3395");
	
	private Config config;
	private ScenarioImpl scenario;
	private List<String> vehicleIds = new ArrayList<String>();
	private boolean useGivenNetwork = false;
	
	public static void main(String[] args) {
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input/sample-feed");
		gtfs.setUseGivenNetwork(false);
		gtfs.convert(6);
	}
	
	
	public GtfsConverter(String filepath){
		this.filepath = filepath;
	}
	
	public Config createConfig(){
		Config c = ConfigUtils.createConfig();
		c.scenario().setUseTransit(true);
		c.scenario().setUseVehicles(true);
		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setStartTime(Time.parseTime("05:30:00"));
		qsim.setEndTime(Time.parseTime("17:00:00"));
		c.addQSimConfigGroup(qsim);
		Set<ControlerConfigGroup.EventsFileFormat> eventsFileFormats = new HashSet<ControlerConfigGroup.EventsFileFormat>();
		eventsFileFormats.add(ControlerConfigGroup.EventsFileFormat.xml);
		c.controler().setEventsFileFormats(eventsFileFormats);		
		Set<String> transitModes = new HashSet<String>();
		transitModes.add("pt");
		c.transit().setTransitModes(transitModes);
		c.transit().setVehiclesFile("../playgrounds/florian/transitVehicles.xml");
		c.transit().setTransitScheduleFile("../playgrounds/florian/transitSchedule.xml");
		c.network().setInputFile("../playgrounds/florian/network.xml");
		ConfigWriter cw = new ConfigWriter(c);
		cw.setPrettyPrint(true);
		cw.write("./config.xml");
		return c;
	}
	
	public void convert(int weekday){
		// 1 = monday, 2 = tuesday,...
		
		// Create a config
		this.config = this.createConfig();		
		this.scenario = (ScenarioImpl)(ScenarioUtils.createScenario(config));
		
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
		
		// Get or Create the Network
		NetworkImpl net;
		if(!useGivenNetwork){
			// Build and safe a Network --- you only need this, if you don't have a network
			System.out.println("Creating Network");
			net = this.createNetworkOfStopsAndTrips(ts);
		}else{
			System.out.println("Reading given Network.");
			new MatsimNetworkReader(scenario).readFile(filepath + "/network.xml");
			net = scenario.getNetwork();
		}		
		new NetworkWriter(net).write("./network.xml");
		System.out.println("Wrote Network to " + new File("./network.xml").getAbsolutePath());
		
		// Assign the links of the Network to the stops - this is only necessary if you use a given network
		if(useGivenNetwork){
			this.assignLinksToStops(ts, net);
		}
		
		// Get the TripRoutes
		Map<Id,NetworkRoute> tripRoute;
		if(useGivenNetwork){
			System.out.println("Create NetworkRoutes");
			tripRoute = createNetworkRoutes2(ts, net);
		}else{
			System.out.println("Create NetworkRoutes");
			tripRoute = createNetworkRoutes(ts, net);
		}
		
		// Convert the schedules for the trips
		System.out.println("Convert the schedules");
		this.convertSchedules(ts, routeNames, routeToTripAssignments, usedTripIds, tripRoute);
		
		// If you use the optional frequencies.txt, it will be transformed here --> not working yet
		if((new File(filepath + "/frequencies.txt")).exists()){
			this.convertFrequencies(ts, routeToTripAssignments, usedTripIds);
		}
		
		// Create some dummy Vehicles
		this.createTransitVehiclesDummy();
		TransitScheduleWriter tsw = new TransitScheduleWriter(ts);
		tsw.writeFile("./transitSchedule.xml");
		System.out.println("Conversion successfull");
	}
	
	private List<Id> getUsedTripIds(List<String> usedServiceIds) {
		String tripsFilename = filepath + "/trips.txt";
		List<Id> usedTripIds = new ArrayList<Id>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(tripsFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int serviceIdIndex = header.indexOf("service_id");
			int tripIdIndex = header.indexOf("trip_id");
			String row = br.readLine();
			do {
				String[] entries = this.splitRow(row);
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
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int routeIdIndex = header.indexOf("route_id");
			int routeLongNameIndex = header.indexOf("route_long_name");
			String row = br.readLine();
			do {
				String[] entries = this.splitRow(row);
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
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int routeIdIndex = header.indexOf("route_id");
			int tripIdIndex = header.indexOf("trip_id");
			String row = br.readLine();
			do {
				String[] entries = this.splitRow(row);
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
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
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
				String[] entries = this.splitRow(row);
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
	
	private void convertFrequencies(TransitSchedule ts, Map<Id, Id> routeToTripAssignments, List<Id> usedTripIds) {
		String frequenciesFilename = filepath + "/frequencies.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(frequenciesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int startTimeIndex = header.indexOf("start_time");
			int endTimeIndex = header.indexOf("end_time");
			int stepIndex = header.indexOf("headway_secs");
			String row = br.readLine();
			int departureCounter = 2;
			String oldTripId = "";
			do {
				String[] entries = this.splitRow(row);
				Id tripId = new IdImpl(entries[tripIdIndex]);
				double startTime = Time.parseTime(entries[startTimeIndex]);
				double endTime = Time.parseTime(entries[endTimeIndex]);
				double step = Double.parseDouble(entries[stepIndex]);
				if((!(entries[tripIdIndex].equals(oldTripId))) && (usedTripIds.contains(tripId))){					
					departureCounter = ts.getTransitLines().get(routeToTripAssignments.get(tripId)).getRoutes().get(tripId).getDepartures().size();
				}
				if(usedTripIds.contains(tripId)){
					Map<Id,Departure> depatures = ts.getTransitLines().get(routeToTripAssignments.get(tripId)).getRoutes().get(tripId).getDepartures();
					double latestDeparture = 0;
					for(Departure d: depatures.values()){
						if(latestDeparture < d.getDepartureTime()){
							latestDeparture = d.getDepartureTime();
						}
					}
					double time = latestDeparture + step;				
					do{
						if(time>startTime){
							Departure d = tf.createDeparture(new IdImpl(tripId.toString() + "." + departureCounter), time);
							d.setVehicleId(new IdImpl(tripId.toString() + "." + departureCounter));
							this.vehicleIds.add(tripId.toString() + "." + departureCounter);
							ts.getTransitLines().get(routeToTripAssignments.get(tripId)).getRoutes().get(tripId).addDeparture(d);
							departureCounter++;
						}						
						time = time + step;
					}while(time <= endTime);		
				}
				oldTripId = entries[tripIdIndex];
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(frequenciesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void convertSchedules(TransitSchedule ts, Map<Id, String> routeNames, Map<Id, Id> routeToTripAssignments, List<Id> tripIds, Map<Id, NetworkRoute> tripRoute){
		String stopTimesFilename = filepath + "/stop_times.txt";
		List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();
		try {
			int idCounter = 0;
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int arrivalTimeIndex = header.indexOf("arrival_time");
			int departureTimeIndex = header.indexOf("departure_time");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			String[] entries = this.splitRow(row);
			String currentTrip = entries[tripIdIndex];
			Double departureTime = Time.parseTime(entries[arrivalTimeIndex]);
			Departure departure = tf.createDeparture(new IdImpl(entries[tripIdIndex] + "." + idCounter), departureTime);
			String vehicleId = entries[tripIdIndex] + "." + idCounter;
			departure.setVehicleId(new IdImpl(vehicleId));		
			this.vehicleIds.add(vehicleId);
			do {				
				entries = this.splitRow(row);
				Id currentTripId = new IdImpl(currentTrip);
				Id tripId = new IdImpl(entries[tripIdIndex]);
				Id stopId = new IdImpl(entries[stopIdIndex]);
				if(tripIds.contains(tripId)){
					if(entries[tripIdIndex].equals(currentTrip)){
						TransitStopFacility stop = ts.getFacilities().get(stopId);
						TransitRouteStop routeStop = tf.createTransitRouteStop(stop, Time.parseTime(entries[arrivalTimeIndex])-departureTime, Time.parseTime(entries[departureTimeIndex])-departureTime);
						routeStop.setAwaitDepartureTime(true);
						stops.add(routeStop);
					}else{
						TransitLine tl = ts.getTransitLines().get(routeToTripAssignments.get(currentTripId));
						TransitRoute tr = tf.createTransitRoute(currentTripId, tripRoute.get(currentTripId), stops, "pt");
						tr.addDeparture(departure);
						tl.addRoute(tr);
						stops = new LinkedList<TransitRouteStop>();
						currentTrip = entries[tripIdIndex];
						departureTime = Time.parseTime(entries[arrivalTimeIndex]);
						departure = tf.createDeparture(new IdImpl(entries[tripIdIndex] + "." + idCounter), departureTime);
						vehicleId = tripId.toString() + "." + idCounter;
						departure.setVehicleId(new IdImpl(vehicleId));
						this.vehicleIds.add(vehicleId);
						TransitStopFacility stop = ts.getFacilities().get(stopId);
						TransitRouteStop routeStop = tf.createTransitRouteStop(stop, Time.parseTime(entries[arrivalTimeIndex])-departureTime, Time.parseTime(entries[departureTimeIndex])-departureTime);
						stops.add(routeStop);
					}
				}		
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
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int stopIdIndex = header.indexOf("stop_id");
			int stopNameIndex = header.indexOf("stop_name");
			int stopLatitudeIndex = header.indexOf("stop_lat");
			int stopLongitudeIndex = header.indexOf("stop_lon");
			String row = br.readLine();
			do{
				String[] entries = this.splitRow(row);
				TransitStopFacility t;
				t = this.tf.createTransitStopFacility(new IdImpl(entries[stopIdIndex]), transform.transform(new CoordImpl(Double.parseDouble(entries[stopLongitudeIndex]), Double.parseDouble(entries[stopLatitudeIndex]))), false);
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
	
	private Map<Id,NetworkRoute> createNetworkRoutes(TransitSchedule ts, NetworkImpl net){
		// this only works, if you used the created network
		String stopTimesFilename = filepath + "/stop_times.txt";
		Map<Id,NetworkRoute> tripRoutes = new HashMap<Id,NetworkRoute>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			String[] entries = row.split(",");			
			LinkedList<Id> route = new LinkedList<Id>();
			String currentTrip = entries[tripIdIndex];
			String startStation = entries[stopIdIndex];
			do {
				entries = this.splitRow(row);
				String nextStation = entries[stopIdIndex];				
				if(currentTrip.equals(entries[tripIdIndex])){					
					if(!(startStation.equals(nextStation))){
						Id linkId = null;
						for(Id fromId: net.getNodes().get(new IdImpl(startStation)).getOutLinks().keySet()){
							for(Id toId: net.getNodes().get(new IdImpl(nextStation)).getInLinks().keySet()){
								if(fromId.equals(toId)){
									linkId = fromId;
								}
							}
						}
						route.add(linkId);
						route.add(new IdImpl("dL2_" + nextStation));
						route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
					}else{
						route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
					}
					startStation = nextStation;
				}else{
					NetworkRoute netRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getFirst(), route.getLast());
					netRoute.setLinkIds(route.getFirst(), route.subList(1, route.size()-1), route.getLast());
					tripRoutes.put(new IdImpl(currentTrip), netRoute);
					currentTrip = entries[tripIdIndex];
					route = new LinkedList<Id>();
					route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
					startStation = entries[stopIdIndex];
				}
				row = br.readLine();
			}while(row!= null);		
			NetworkRoute netRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getFirst(), route.getLast());
			netRoute.setLinkIds(route.getFirst(), route, route.getLast());
			tripRoutes.put(new IdImpl(currentTrip), netRoute);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//LinkNetworkRoute net = (new LinkNetworkRouteFactory()).createRoute(startLinkId, endLinkId);
		return tripRoutes;		
	}
	
	private Map<Id,NetworkRoute> createNetworkRoutes2(TransitSchedule ts, NetworkImpl net){
		String stopTimesFilename = filepath + "/stop_times.txt";
		Map<Id,NetworkRoute> tripRoutes = new HashMap<Id,NetworkRoute>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			String[] entries = this.splitRow(row);
			String currentTrip = entries[tripIdIndex];
			TransitStopFacility start = ts.getFacilities().get(new IdImpl(entries[stopIdIndex]));
			TransitStopFacility end = ts.getFacilities().get(new IdImpl(entries[stopIdIndex]));
			do {
				entries = this.splitRow(row);
				if(!(currentTrip.equals(entries[tripIdIndex]))){								
					LeastCostPathCalculator routingAlgo = new Dijkstra(net, new FreespeedTravelTimeCost(ConfigUtils.createConfig().planCalcScore()), new FreespeedTravelTimeCost(ConfigUtils.createConfig().planCalcScore()));
//					Path path = routingAlgo.calcLeastCostPath(net.getNearestNode(start.getCoord()), net.getNearestNode(end.getCoord()), 0.0);
					Path path = routingAlgo.calcLeastCostPath(net.getLinks().get(start.getLinkId()).getToNode(), net.getNearestNode(end.getCoord()), 0.0);
					if (path == null) {
						throw new RuntimeException("No route found from facility " + start.getId() + " to " + end.getId() + ".");
					}
					NetworkRoute route = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(start.getLinkId(), end.getLinkId());
					route.setLinkIds(start.getLinkId(), NetworkUtils.getLinkIds(path.links), end.getLinkId());
					tripRoutes.put(new IdImpl(currentTrip), route);
					currentTrip = entries[tripIdIndex];
					start = ts.getFacilities().get(new IdImpl(entries[stopIdIndex]));
				}else{
					end = ts.getFacilities().get(new IdImpl(entries[stopIdIndex]));
				}
				row = br.readLine();
			}while(row!= null);		
			LeastCostPathCalculator routingAlgo = new Dijkstra(net, new FreespeedTravelTimeCost(ConfigUtils.createConfig().planCalcScore()), new FreespeedTravelTimeCost(ConfigUtils.createConfig().planCalcScore()));
//			Path path = routingAlgo.calcLeastCostPath(net.getNearestNode(start.getCoord()), net.getNearestNode(end.getCoord()), 0.0);
			Path path = routingAlgo.calcLeastCostPath(net.getLinks().get(start.getLinkId()).getToNode(), net.getNearestNode(end.getCoord()), 0.0);
			if (path == null) {
				throw new RuntimeException("No route found from facility " + start.getId() + " to " + end.getId() + ".");
			}
			NetworkRoute route = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(start.getLinkId(), end.getLinkId());
			route.setLinkIds(start.getLinkId(), NetworkUtils.getLinkIds(path.links), end.getLinkId());
			tripRoutes.put(new IdImpl(currentTrip), route);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//LinkNetworkRoute net = (new LinkNetworkRouteFactory()).createRoute(startLinkId, endLinkId);
		return tripRoutes;		
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
		double capacity = 1500.0;
		double freespeed = freespeedKmPerHour / 3.6;
		int numLanes = 1;
		long i = 0;
		// To prevent the creation of similiar links in different directions there need to be a Map which assigns the ToNodes to all FromNodes
		Map<Id,List<Id>> fromNodes = new HashMap<Id,List<Id>>();
		// Create a new Network
		NetworkImpl network = scenario.getNetwork();
		// Add all stops as nodes but move them a little
		Map<Id, TransitStopFacility> stops = ts.getFacilities();
		for(Id id: stops.keySet()){
			TransitStopFacility stop = stops.get(id);
			NodeImpl n = new NodeImpl(id);
			n.setCoord(new CoordImpl(stop.getCoord().getX()-1,stop.getCoord().getY()-1));
			network.addNode(n);
			stop.setLinkId(new IdImpl("dL1_"+ stop.getId().toString()));
		}
		// Get the Links from the trips in stop_times.txt
		String stopTimesFilename = filepath + "/stop_times.txt";
		Map<Id, Node> nodes = network.getNodes();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			do {
				boolean addLink = false;
				String[] entries = this.splitRow(row);
				Id fromNodeId = new IdImpl(entries[stopIdIndex]); // This works only as long as the nodes have the same ID as the stops;
				String usedTripId = entries[tripIdIndex];
				row = br.readLine();
				if(row!=null){
					entries = this.splitRow(row);
					Id toNodeId = new IdImpl(entries[stopIdIndex]);
					if(fromNodes.containsKey(fromNodeId)){
						if(!(fromNodes.get(fromNodeId)).contains(toNodeId)){
							addLink = true;
						}
					}else{
						addLink = true;
						fromNodes.put(fromNodeId, new ArrayList<Id>());
					}
					if(!(fromNodes.containsKey(toNodeId))){
						fromNodes.put(toNodeId, new ArrayList<Id>());
					}
					// No 0-Length Links
					if(fromNodeId.equals(toNodeId)){
						addLink = false;
					}
					// If the toNode belongs to a different trip, there should not be a link!
					if(!usedTripId.equals(entries[tripIdIndex])){
						addLink = false;
					}
					// for each stop should exist one dummyLink --> does it need one?
					Id dummyId = new IdImpl("dN_" + toNodeId);
					if(!(network.getNodes().containsKey(dummyId))){
						NodeImpl n = new NodeImpl(dummyId);
						n.setCoord(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getCoord());
//						n.setCoord(new CoordImpl(nodes.get(toNodeId).getCoord().getX()+10,nodes.get(toNodeId).getCoord().getY()+10));
						network.addNode(n);
						double length = CoordUtils.calcDistance(n.getCoord(), nodes.get(toNodeId).getCoord());
						Link link = network.createAndAddLink(new IdImpl("dL1_" + toNodeId), n, nodes.get(toNodeId), length, freespeed, capacity, numLanes);
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						// Backwards
						Link link2 = network.createAndAddLink(new IdImpl("dL2_" + toNodeId), nodes.get(toNodeId),n , length, freespeed, capacity, numLanes);
						link2.setAllowedModes(modes);
					}
					if(addLink){
						double length = CoordUtils.calcDistance(nodes.get(fromNodeId).getCoord(), nodes.get(toNodeId).getCoord());
						Link link = network.createAndAddLink(new IdImpl(i++), nodes.get(fromNodeId), nodes.get(toNodeId), length, freespeed, capacity, numLanes);						
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						// Backwards
						Link link2 = network.createAndAddLink(new IdImpl(i++), nodes.get(toNodeId), nodes.get(fromNodeId), length, freespeed, capacity, numLanes);
						link2.setAllowedModes(modes);
						fromNodes.get(fromNodeId).add(toNodeId);
						fromNodes.get(toNodeId).add(fromNodeId);
					}					
				}		
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
//		(new NetworkCleaner()).run(network);
		return network;
	}


	private void createTransitVehiclesDummy(){
		VehiclesFactory vf = new VehiclesFactoryImpl();
		// TYPE
		VehicleType vt = vf.createVehicleType(new IdImpl("dummyType"));
		vt.setDescription("Dummy Vehicle Type for GTFS Converter");
		VehicleCapacity vc = vf.createVehicleCapacity();
		vc.setSeats(50);
		vc.setStandingRoom(50);
		vt.setCapacity(vc);
		vt.setLength(5);
		// VEHICLE
		Vehicles vs = scenario.getVehicles();
		vs.getVehicleTypes().put(new IdImpl("dummyType"), vt);
		for(String s: vehicleIds){
			Vehicle v = vf.createVehicle(new IdImpl(s), vt);
			vs.getVehicles().put(new IdImpl(s), v);
		}	
		VehicleWriterV1 vw = new VehicleWriterV1(vs);
		vw.writeFile("./transitVehicles.xml");
	}
	
	private void setUseGivenNetwork(boolean b) {
		this.useGivenNetwork  = b;
		
	}


	private String[] splitRow(String row){
		List<String> entries = new ArrayList<String>();
		boolean quotes = false;
		StringBuilder sb = new StringBuilder();
		for(int i =0; i<row.length(); i++){
			if(row.charAt(i) == '"'){
				quotes = !(quotes);
			}else if((row.charAt(i) == ',') && !(quotes)){
				entries.add(sb.toString());	
				sb = new StringBuilder();
			}else{
				sb.append(row.charAt(i));
			}
		}
		entries.add(sb.toString());	
		return entries.toArray(new String[entries.size()]);
	}

}
