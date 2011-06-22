package playground.florian.GTFSConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
	private boolean createShapedNetwork = false;
	
	private long date = 0;
	
	
	// Fields for shaped Network
	// (TripId,ShapeId)
	Map<String,String> shapeIdToTripIdAssignments = new HashMap<String,String>();
	// (LinkId,(TripId,FromShapeDist,ToShapeDist))
	Map<Id,String[]> shapedLinkIds = new HashMap<Id,String[]>();
	// If there is no shape_dist_traveled field, try to identify the stations by its coordinates
	// (LinkId,(TripId,FromCoordX, FromCoordY ,ToCoordX, ToCoordY)) - Both Coordinates as Strings and in Matsim-KS
	Map<Id,String[]> shapedLinkIdsCoordinate = new HashMap<Id,String[]>();
	boolean alternativeStationToShapeAssignment = false;
	double toleranceInM = 0;
	// (ShapeId, (shapeDist, x, y))
	Map<Id,List<String[]>> shapes = new HashMap<Id,List<String[]>>();
	
	
	public static void main(String[] args) {
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input/sample-feed");
		gtfs.setUseGivenNetwork(false);
		gtfs.setCreateShapedNetwork(false);
//		gtfs.setDate(20110711);
		gtfs.convert();
	}
	
	
	public GtfsConverter(String filepath){
		this.filepath = filepath;
	}
	
	public Config createConfig(){
		Config c = ConfigUtils.createConfig();
		c.scenario().setUseTransit(true);
		c.scenario().setUseVehicles(true);
		QSimConfigGroup qsim = new QSimConfigGroup();
//		qsim.setStartTime(Time.parseTime("00:00:00"));
//		qsim.setEndTime(Time.parseTime("17:00:00"));
		c.addQSimConfigGroup(qsim);
		Set<ControlerConfigGroup.EventsFileFormat> eventsFileFormats = new HashSet<ControlerConfigGroup.EventsFileFormat>();
		eventsFileFormats.add(ControlerConfigGroup.EventsFileFormat.xml);
		c.controler().setEventsFileFormats(eventsFileFormats);		
		Set<String> transitModes = new HashSet<String>();
		transitModes.add("pt");
		c.transit().setTransitModes(transitModes);
		c.transit().setVehiclesFile("../playgrounds/florian/transitVehicles.xml");
		c.transit().setTransitScheduleFile("../playgrounds/florian/transitSchedule.xml");
		if(this.createShapedNetwork){
			c.network().setInputFile("../playgrounds/florian/shapedNetwork.xml");
		}else{
			c.network().setInputFile("../playgrounds/florian/network.xml");
		}
		ConfigWriter cw = new ConfigWriter(c);
		cw.setPrettyPrint(true);
		cw.write("./config.xml");
		return c;
	}
	
	public void convert(){		
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
		
		// Get the used service Id for the choosen weekday and date
		List<String> usedServiceIds = new ArrayList<String>();
		if((new File(filepath + "/calendar.txt")).exists()){
			System.out.println("Reading calendar.txt");
			usedServiceIds.addAll(this.getUsedServiceIds());
		}
		if((new File(filepath + "/calendar_dates.txt")).exists()){
			System.out.println("Reading calendar_dates.txt");
			for(String serviceId: this.getUsedServiceIdsForSpecialDates()){
				if(serviceId.charAt(0) == '+'){
					usedServiceIds.add(serviceId.substring(1));
				}else{
					if(usedServiceIds.contains(serviceId.substring(1))){
						usedServiceIds.remove(serviceId.substring(1));
					}
				}
			}
		}		
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
		
		// Create the shaped Network
		Map<Id,NetworkRoute> shapedTripRoute = null;
		if(this.createShapedNetwork){
			System.out.println("Creating shaped Network");
			this.convertShapes();
			Map<Id, List<Coord>> shapedLinks = this.assignShapesToLinks();
			Object[] results = this.replaceLinks(shapedLinks,net,tripRoute);
			shapedTripRoute = (Map<Id,NetworkRoute>)(results[1]);
			new NetworkWriter((Network)results[0]).write("./shapedNetwork.xml");
			System.out.println("Wrote shaped Network to " + new File("./shapedNetwork.xml").getAbsolutePath());			
		}
		
		// Convert the schedules for the trips
		System.out.println("Convert the schedules");
		if(this.createShapedNetwork){
			this.convertSchedules(ts, routeNames, routeToTripAssignments, usedTripIds, shapedTripRoute);
		}else{
			this.convertSchedules(ts, routeNames, routeToTripAssignments, usedTripIds, tripRoute);
		}		
		
		// If you use the optional frequencies.txt, it will be transformed here
		if((new File(filepath + "/frequencies.txt")).exists()){
			this.convertFrequencies(ts, routeToTripAssignments, usedTripIds);
		}
		
		// Create some dummy Vehicles
		this.createTransitVehiclesDummy();
		System.out.println("Writing TransitSchedule.");
		TransitScheduleWriter tsw = new TransitScheduleWriter(ts);
		tsw.writeFile("./transitSchedule.xml");
		if(usedTripIds.isEmpty()){
			System.out.println("There are no converted trips. You might need to change the date for better results.");
		}
		System.out.println("Conversion successfull");
	}
	
	public void setDate(long date) {
		this.date = date;
	}


	public void setCreateShapedNetwork(boolean createShapedNetwork) {
		if(((new File(filepath + "/shapes.txt")).exists()) && (createShapedNetwork)){
			this.createShapedNetwork = createShapedNetwork;
		}else if(createShapedNetwork){
			System.out.println("Couldn't find the 'shapes.txt'. No shaped network will be created!");
		}else if(!createShapedNetwork){
			this.createShapedNetwork = createShapedNetwork;
		}
	}


	public void setUseGivenNetwork(boolean b) {
		if(((new File(filepath + "/network.txt")).exists()) && (b)){
			this.useGivenNetwork  = b;
		}else if(b){
			System.out.println("Couldn't find the 'network.txt'. The Converter will create its own network!");
		}		
	}


	private List<Id> getUsedTripIds(List<String> usedServiceIds) {
		String tripsFilename = filepath + "/trips.txt";
		List<Id> usedTripIds = new ArrayList<Id>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(tripsFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int serviceIdIndex = header.indexOf("service_id");
			int tripIdIndex = header.indexOf("trip_id");
			int shapeIdIndex = header.indexOf("shape_id");
			String row = br.readLine();
			do {
				String[] entries = this.splitRow(row);
				if(usedServiceIds.contains(entries[serviceIdIndex])){
					usedTripIds.add(new IdImpl(entries[tripIdIndex]));
				}
				if(this.createShapedNetwork){
					if(shapeIdIndex > 0){
						String shapeId = entries[shapeIdIndex];
						this.shapeIdToTripIdAssignments.put(entries[tripIdIndex], shapeId);
					}else{
						System.out.println("WARNING: Couldn't find shape_id header in trips.txt. Deactivating creation of shaped network");
						this.createShapedNetwork = false;
					}
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

	private List<String> getUsedServiceIds(){
		String calendarFilename = filepath + "/calendar.txt";
		List<String> serviceIds = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(calendarFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int serviceIdIndex = header.indexOf("service_id");
			int startDateIndex = header.indexOf("start_date");
			int endDateIndex = header.indexOf("end_date");
			int[] weekdayIndexes= new int[7];
			weekdayIndexes[0] = header.indexOf("monday");
			weekdayIndexes[1] = header.indexOf("tuesday");
			weekdayIndexes[2] = header.indexOf("wednesday");
			weekdayIndexes[3] = header.indexOf("thursday");
			weekdayIndexes[4] = header.indexOf("friday");
			weekdayIndexes[5] = header.indexOf("saturday");
			weekdayIndexes[6] = header.indexOf("sunday");
			String row = br.readLine();
			int weekday;
			do {
				String[] entries = this.splitRow(row);
				if(this.date == 0){
					this.date = Long.parseLong(entries[endDateIndex].trim());
					weekday = this.getWeekday(date);
					System.out.println("Used Date for active schedules: " + this.date + " (weekday: " + weekday + "). If you want to choose another date, please specify it, before running the converter");
				}else{
					weekday = this.getWeekday(date);
				}
				if(entries[weekdayIndexes[weekday-1]].equals("1")){
					if((this.date >= Double.parseDouble(entries[startDateIndex].trim())) && (this.date <= Double.parseDouble(entries[endDateIndex].trim()))){
						serviceIds.add(entries[serviceIdIndex]);
					}
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
	
	private List<String> getUsedServiceIdsForSpecialDates(){
		String calendarFilename = filepath + "/calendar_dates.txt";
		List<String> serviceIds = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(calendarFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int serviceIdIndex = header.indexOf("service_id");
			int dateIndex = header.indexOf("date");
			int exceptionTypeIndex = header.indexOf("exception_type");
			String row = br.readLine();
			if(row!= null){
				do {
					String[] entries = this.splitRow(row);
					String serviceId = entries[serviceIdIndex];
					long exceptionDate = Long.parseLong(entries[dateIndex].trim());
					int exceptionType = Integer.parseInt(entries[exceptionTypeIndex].trim());
					if(this.date == 0){
						this.date = exceptionDate;
						System.out.println("Used Date for active schedules: " + this.date + ". If you want to choose another date, please specify it, before running the converter");
					}
					if(exceptionDate == this.date){
						if(exceptionType == 1){
							serviceIds.add("+" + serviceId);
						}else{
							serviceIds.add("-" + serviceId);
						}
					}
					row = br.readLine();
				}while(row!= null);
			}			
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
			if(row != null){
				do {
					String[] entries = this.splitRow(row);
					Id tripId = new IdImpl(entries[tripIdIndex]);
					double startTime = Time.parseTime(entries[startTimeIndex].trim());
					double endTime = Time.parseTime(entries[endTimeIndex].trim());
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
			}
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
			Double departureTime = Time.parseTime(entries[arrivalTimeIndex].trim());
			Departure departure = tf.createDeparture(new IdImpl(entries[tripIdIndex] + "." + idCounter), departureTime);
			String vehicleId = entries[tripIdIndex] + "." + idCounter;
			departure.setVehicleId(new IdImpl(vehicleId));		
			this.vehicleIds.add(vehicleId);
			do {				
				entries = this.splitRow(row);
				Id currentTripId = new IdImpl(currentTrip);
				Id tripId = new IdImpl(entries[tripIdIndex]);
				Id stopId = new IdImpl(entries[stopIdIndex]);
				if(entries[tripIdIndex].equals(currentTrip)){
					if(tripIds.contains(tripId)){
							TransitStopFacility stop = ts.getFacilities().get(stopId);
							TransitRouteStop routeStop = tf.createTransitRouteStop(stop, Time.parseTime(entries[arrivalTimeIndex].trim())-departureTime, Time.parseTime(entries[departureTimeIndex].trim())-departureTime);
							routeStop.setAwaitDepartureTime(true);
							stops.add(routeStop);		
					}
				}else{
					if(tripIds.contains(currentTripId)){
						//finish old route
						TransitLine tl = ts.getTransitLines().get(routeToTripAssignments.get(currentTripId));
						TransitRoute tr = tf.createTransitRoute(currentTripId, tripRoute.get(currentTripId), stops, "pt");
						tr.addDeparture(departure);
						tl.addRoute(tr);
						stops = new LinkedList<TransitRouteStop>();
					}
					if(tripIds.contains(tripId)){
						//begin new route
						departureTime = Time.parseTime(entries[arrivalTimeIndex].trim());
						departure = tf.createDeparture(new IdImpl(entries[tripIdIndex] + "." + idCounter), departureTime);
						vehicleId = tripId.toString() + "." + idCounter;
						departure.setVehicleId(new IdImpl(vehicleId));
						this.vehicleIds.add(vehicleId);												
						TransitStopFacility stop = ts.getFacilities().get(stopId);
						TransitRouteStop routeStop = tf.createTransitRouteStop(stop, 0, Time.parseTime(entries[departureTimeIndex].trim())-departureTime);
						stops.add(routeStop);
					}
					currentTrip = entries[tripIdIndex];						
				}						
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void convertShapes(){
		String frequenciesFilename = filepath + "/shapes.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(frequenciesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine())));
			int shapeIdIndex = header.indexOf("shape_id");
			int shapeLatIndex = header.indexOf("shape_pt_lat");
			int shapeLonIndex = header.indexOf("shape_pt_lon");
			int shapeDistIndex = header.indexOf("shape_dist_traveled");
			if((shapeDistIndex<0) && this.createShapedNetwork){
				this.alternativeStationToShapeAssignment = true;
				System.out.println("Couldn't find the shape_dist_traveled field in shapes.txt. Now it uses the alternative station to shape assingment.");
			}
			String row = br.readLine();
			String[] entries = this.splitRow(row);
			String oldShapeId = entries[shapeIdIndex];
			List<String[]> shapes = new ArrayList<String[]>();
			do {
				entries = this.splitRow(row);
				String shapeId = entries[shapeIdIndex];
				String shapeLat = entries[shapeLatIndex];
				String shapeLon = entries[shapeLonIndex];
				String shapeDist;
				if(alternativeStationToShapeAssignment){
					shapeDist = "Alternative Station To Shape Assignment Is Used";
				}else{
					shapeDist = entries[shapeDistIndex];
				}
				if(oldShapeId.equals(shapeId)){
					String[] params = new String[3];
					params[0] = shapeDist;
					params[1] = shapeLon;
					params[2] = shapeLat;
					shapes.add(params);
				}else{
					this.shapes.put(new IdImpl(oldShapeId), shapes);
					oldShapeId = shapeId;
					shapes = new ArrayList<String[]>();
					String[] params = new String[3];
					params[0] = shapeDist;
					params[1] = shapeLon;
					params[2] = shapeLat;
					shapes.add(params);
				}
				row = br.readLine();
			}while(row!= null);
			this.shapes.put(new IdImpl(oldShapeId), shapes);
		} catch (FileNotFoundException e) {
			System.out.println(frequenciesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<Id,List<Coord>> assignShapesToLinks() {
		Map<Id,List<Coord>> result = new HashMap<Id, List<Coord>>();
		Map<Id,String[]> linkIdInfos = null;
		if(this.alternativeStationToShapeAssignment){
			linkIdInfos = this.shapedLinkIdsCoordinate;			
		}else{
			linkIdInfos = this.shapedLinkIds;
		}
		for(Id linkId: linkIdInfos.keySet()){
			String[] params = linkIdInfos.get(linkId);
			if(params[2].isEmpty()){
				System.out.println("There might be a problem with shape_dist_traveled field of the trip " + params[0]);
			}
			String tripId = params[0];
			List<Coord> coord = new ArrayList<Coord>();
			if(!this.alternativeStationToShapeAssignment){
				Double shapeDistStart = Double.parseDouble(params[1].trim());
				Double shapeDistEnd = Double.parseDouble(params[2].trim());
				String shapeId = this.shapeIdToTripIdAssignments.get(tripId);
				List<String[]> shapes = this.shapes.get(new IdImpl(shapeId));		
				for(String[] shapeCoord: shapes){
					double dist = Double.parseDouble(shapeCoord[0].trim());
					if((shapeDistStart <= dist) && (shapeDistEnd >= dist)){
						 coord.add(transform.transform(new CoordImpl(Double.parseDouble(shapeCoord[1]), Double.parseDouble(shapeCoord[2]))));
					}
				}
			}else{
				Coord fromCoord = new CoordImpl(params[1], params[2]);
				Coord toCoord = new CoordImpl(params[3], params[4]);
				String shapeId = this.shapeIdToTripIdAssignments.get(tripId);
				List<String[]> shapes = this.shapes.get(new IdImpl(shapeId));
				boolean add = false;
				for(String[] shapeCoord: shapes){
					Coord c = transform.transform(new CoordImpl(Double.parseDouble(shapeCoord[1]), Double.parseDouble(shapeCoord[2])));
					if(CoordUtils.calcDistance(c, fromCoord) <= this.toleranceInM){
						add = true;						 
					}else if(CoordUtils.calcDistance(c, toCoord) <= this.toleranceInM){
						add = false;
						coord.add(c);
						break;
					}
					if(add){
						coord.add(c);						
					}
				}
			}
			if(!coord.isEmpty()){
				result.put(linkId, coord);
			}else{
				result.put(linkId, coord);
				System.out.println("Couldn't find any shapes for Link " + linkId + ". This Link will not be shaped.");
			}			
		}
		return result;
	}


	private Object[] replaceLinks(Map<Id, List<Coord>> shapedLinks, NetworkImpl net, Map<Id,NetworkRoute> netRoutes) {
		ScenarioImpl sc = (ScenarioImpl)(ScenarioUtils.createScenario(config));
		NetworkImpl net2 = sc.getNetwork();
		Map<Id,List<Id>> replacedLinks = new HashMap<Id,List<Id>>();
		Map<Id,NetworkRoute> netRoutes2 = new HashMap<Id,NetworkRoute>();
		Map<Id,List<Id>> fromNodes = new HashMap<Id,List<Id>>();
		Map<Coord,Node> existingNodes = new HashMap<Coord,Node>();
		// Copy Nodes from original Network to new Network
		for(Id id: net.getNodes().keySet()){
			net2.createAndAddNode(id, net.getNodes().get(id).getCoord());
			existingNodes.put(net2.getNodes().get(id).getCoord(), net2.getNodes().get(id));
		}
		// Replace Links
		for(Id linkId: net.getLinks().keySet()){
			Link link = net.getLinks().get(linkId);
			if(shapedLinks.containsKey(linkId)){				
				List<Id> newLinks = new ArrayList<Id>();
				Node n1 = net2.getNodes().get(link.getFromNode().getId());
				Node n2;
				int shapeCounter = 1;
				for(Coord x: shapedLinks.get(linkId)){
					boolean addLink = false;
					if(existingNodes.containsKey(x)){
						n2 = existingNodes.get(x);
					}else{
						n2 = net2.createAndAddNode(new IdImpl("s" + linkId.toString() + "." + shapeCounter), x);
						existingNodes.put(x, n2);
					}
					if(fromNodes.containsKey(n1.getId())){
						if(!(fromNodes.get(n1.getId()).contains(n2.getId()))){
							addLink = true;
						}else{
							newLinks.add(this.getLinkBetweenNodes(net2,n1.getId(),n2.getId()).getId());
						}
					}else{
						fromNodes.put(n1.getId(), new ArrayList<Id>());
						addLink = true;
					}
					if(n1.getCoord().equals(x)){
						addLink = false;
					}
					if(addLink){
						double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());						
						double freespeed = 50/3.6;
						Link newLink = net2.createAndAddLink(new IdImpl(linkId + "." + shapeCounter), n1, n2, length, freespeed, 1500, 1);											
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						fromNodes.get(n1.getId()).add(n2.getId());
						newLinks.add(newLink.getId());
						shapeCounter++;						
					}
					n1 = n2;
				}
				n2 = link.getToNode();
				if(!n2.getCoord().equals(n1.getCoord())){
					if(fromNodes.containsKey(n1.getId())){
						if(!(fromNodes.get(n1.getId()).contains(n2.getId()))){
							double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());
							double freespeed = 50/3.6;
							Link newLink = net2.createAndAddLink(new IdImpl(linkId + "." + shapeCounter), n1, n2, length, freespeed, 1500, 1);						
							// Change the linktype to pt
							Set<String> modes = new HashSet<String>();
							modes.add(TransportMode.pt);
							link.setAllowedModes(modes);
							newLinks.add(newLink.getId());
						}else{
							newLinks.add(this.getLinkBetweenNodes(net2,n1.getId(),n2.getId()).getId());
						}
					}else{
						fromNodes.put(n1.getId(), new ArrayList<Id>());
						double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());
						double freespeed = 50/3.6;
						Link newLink = net2.createAndAddLink(new IdImpl(linkId + "." + shapeCounter), n1, n2, length, freespeed, 1500, 1);						
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						newLinks.add(newLink.getId());
					}
					fromNodes.get(n1.getId()).add(n2.getId());
				}								
				replacedLinks.put(linkId, newLinks);
			}else{
				if(!net2.getLinks().containsKey(link.getId())){
					Link newLink = net2.createAndAddLink(link.getId(), net2.getNodes().get(link.getFromNode().getId()), net2.getNodes().get(link.getToNode().getId()), link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());						
					// Change the linktype to pt
					Set<String> modes = new HashSet<String>();
					modes.add(TransportMode.pt);
					newLink.setAllowedModes(modes);
					if(fromNodes.containsKey(newLink.getFromNode().getId())){
						fromNodes.get(newLink.getFromNode().getId()).add(newLink.getToNode().getId());
					}else{
						List<Id> toNodes = new ArrayList<Id>();
						toNodes.add(newLink.getToNode().getId());
						fromNodes.put(newLink.getFromNode().getId(), toNodes);
					}
				}
			}
		}
		// Calculate Freespeed
		for(Id id: replacedLinks.keySet()){
			double oldLength = net.getLinks().get(id).getLength();
			double oldFreespeed = net.getLinks().get(id).getFreespeed();
			double newLength = 0;
			if ((oldFreespeed > 0) && (oldFreespeed != 50/3.6) && (oldLength != 0)){
				for(Id newId: replacedLinks.get(id)){
					newLength += net2.getLinks().get(newId).getLength();
				}
				double newFreespeed = oldFreespeed * newLength / oldLength;
				for(Id newId: replacedLinks.get(id)){
					net2.getLinks().get(newId).setFreespeed(newFreespeed);
				}
			}
		}
		// ReplaceLinks in Netroute
		for(Id id: netRoutes.keySet()){
			NetworkRoute route = netRoutes.get(id);
			LinkedList<Id> routeIds = new LinkedList<Id>();
			for(Id routedLinkId: route.getLinkIds()){
				if(replacedLinks.containsKey(routedLinkId)){
					routeIds.addAll(replacedLinks.get(routedLinkId));
				}else{
					routeIds.add(routedLinkId);
				}
			}
			NetworkRoute newRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getStartLinkId(), route.getEndLinkId());
			newRoute.setLinkIds(route.getStartLinkId(), routeIds, route.getEndLinkId());
			netRoutes2.put(id, newRoute);
		}
		Object[] results = new Object[2];
		results[0] = net2;
		results[1] = netRoutes2;
		return results;
	}


	private void convertStops(TransitSchedule ts){
		String filename = filepath + "/stops.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			List<String> header = new ArrayList<String>(Arrays.asList(this.splitRow(br.readLine().trim())));
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
					if(route.size() > 2){
						netRoute.setLinkIds(route.getFirst(), route.subList(1, route.size()-1), route.getLast());
					}
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
//		double freespeed = freespeedKmPerHour / 3.6;
		int numLanes = 1;
		long i = 0;
		// To prevent the creation of similiar links in different directions there need to be a Map which assigns the ToNodes to all FromNodes
		Map<Id,List<Id>> fromNodes = new HashMap<Id,List<Id>>();
		// Create a new Network
		NetworkImpl network = scenario.getNetwork();
		// Add all stops as nodes
		Map<Id, TransitStopFacility> stops = ts.getFacilities();
		for(Id id: stops.keySet()){
			TransitStopFacility stop = stops.get(id);
			NodeImpl n = new NodeImpl(id);
			n.setCoord(stop.getCoord());
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
			int arrivalTimeIndex = header.indexOf("arrival_time");
			int departureTimeIndex = header.indexOf("departure_time");
			int shapeDistIndex = header.indexOf("shape_dist_traveled");
			if(shapeDistIndex < 0){
				System.out.println("Couldn't find the shape_dist_traveled field in stop_times.txt. Now it uses the alternative station to shape assingment.");
				this.alternativeStationToShapeAssignment = true;
			}
			String row = br.readLine();
			do {
				boolean addLink = false;
				String[] entries = this.splitRow(row);
				Id fromNodeId = new IdImpl(entries[stopIdIndex]);
				double departureTime = Time.parseTime(entries[departureTimeIndex].trim());
				String usedTripId = entries[tripIdIndex];
				String fromShapeDist = "";
				Coord fromShapeCoord = null;				
				// Prepare the replacing with shaped links
				if(createShapedNetwork){
					if(!this.alternativeStationToShapeAssignment){
						fromShapeDist = entries[shapeDistIndex];
						if(fromShapeDist.isEmpty()){
							fromShapeDist = "0.0";
						}
					}else{
//						WARNING: Couldn't find shape_dist_traveled header in stop_times.txt. The converter will try to identify the Stations by its coordinates.
						this.alternativeStationToShapeAssignment = true;
						fromShapeCoord = network.getNodes().get(fromNodeId).getCoord();
					}
				}
				row = br.readLine();
				if(row!=null){
					entries = this.splitRow(row);
					Id toNodeId = new IdImpl(entries[stopIdIndex]);
					double arrivalTime = Time.parseTime(entries[arrivalTimeIndex].trim());
					String toShapeDist = "";
					Coord toShapeCoord = null;
					// Prepare the replacing with shaped links
					if(createShapedNetwork){
						if(!this.alternativeStationToShapeAssignment){
							toShapeDist = entries[shapeDistIndex];
						}else{
							toShapeCoord = network.getNodes().get(toNodeId).getCoord();
						}						
					}
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
					// for each stop should exist one dummyLink
					Id dummyId = new IdImpl("dN_" + toNodeId);
					if(!(network.getNodes().containsKey(dummyId))){
						NodeImpl n = new NodeImpl(dummyId);
						n.setCoord(new CoordImpl(nodes.get(toNodeId).getCoord().getX()+1,nodes.get(toNodeId).getCoord().getY()+1));
						network.addNode(n);
						double length = CoordUtils.calcDistance(n.getCoord(), nodes.get(toNodeId).getCoord());
						Link link = network.createAndAddLink(new IdImpl("dL1_" + toNodeId), n, nodes.get(toNodeId), length, 1000, capacity, numLanes);
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						// Backwards
						Link link2 = network.createAndAddLink(new IdImpl("dL2_" + toNodeId), nodes.get(toNodeId),n , length, 1000, capacity, numLanes);
						link2.setAllowedModes(modes);
					}
					Link link = null;
					if(addLink){
						double length = CoordUtils.calcDistance(nodes.get(fromNodeId).getCoord(), nodes.get(toNodeId).getCoord());
						Double freespeed = freespeedKmPerHour/3.6;
						if((length > 0.0) && (departureTime != 0) && (arrivalTime != 0)){
							freespeed = length/(arrivalTime - departureTime);
							if(freespeed.isInfinite()){
								freespeed = 50/3.6;
								System.out.println("The Difference between ArrivalTime at one Stop (" + toNodeId + ") and DepartureTime at the previous Stop (" + fromNodeId + ") is 0. That leads to high freespeeds.");
							}
						}
						link = network.createAndAddLink(new IdImpl(i++), nodes.get(fromNodeId), nodes.get(toNodeId), length, freespeed, capacity, numLanes);						
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						fromNodes.get(fromNodeId).add(toNodeId);
						// Prepare the replacing with shaped links
						if(createShapedNetwork){
							if(!alternativeStationToShapeAssignment){
								String[] shapeInfos = new String[3];
								shapeInfos[0] = usedTripId;
								shapeInfos[1] = fromShapeDist;
								shapeInfos[2] = toShapeDist;
								this.shapedLinkIds.put(link.getId(), shapeInfos);
							}else{
								String[] shapeInfos = new String[5];
								shapeInfos[0] = usedTripId;
								shapeInfos[1] = String.valueOf(fromShapeCoord.getX());
								shapeInfos[2] = String.valueOf(fromShapeCoord.getY());
								shapeInfos[3] = String.valueOf(toShapeCoord.getX());
								shapeInfos[4] = String.valueOf(toShapeCoord.getY());
								this.shapedLinkIdsCoordinate.put(link.getId(), shapeInfos);
							}							
						}						
						// Backwards
//						Link link2 = network.createAndAddLink(new IdImpl(i++), nodes.get(toNodeId), nodes.get(fromNodeId), length, freespeed, capacity, numLanes);
//						link2.setAllowedModes(modes);						
//						fromNodes.get(toNodeId).add(fromNodeId);
					}									
				}		
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
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
//		vt.setMaximumVelocity(80/3.6);
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
	
	private int getWeekday(long date) {
		int year = (int)(date/10000);
		int month = (int)((date - year*10000)/100);
		int day = (int)((date-month*100-year*10000));
		Calendar cal = new GregorianCalendar(year,month-1,day);
		int weekday = cal.get(Calendar.DAY_OF_WEEK)-1;
		if(weekday < 1){
			return 7;
		}else{
			return weekday;
		}		
	}


	private Link getLinkBetweenNodes(NetworkImpl net2, Id fromId, Id toId) {
		Link result = null;
		for(Link l: net2.getNodes().get(fromId).getOutLinks().values()){
			if(l.getToNode().getId().equals(toId)){
				result = l;
			}
		}
		if(result == null){
			System.out.println("Couldn't find a link between " + fromId + " and " + toId);
		}
		return result;
	}


	private String[] splitRow(String row){
		List<String> entries = new ArrayList<String>();
		boolean quotes = false;
		StringBuilder sb = new StringBuilder();
		for(int i =0; i<row.length(); i++){
			if(row.charAt(i) == '"'){
				quotes = !(quotes);
			}else if((row.charAt(i) == ',') && !(quotes)){
				entries.add(sb.toString().trim());	
				sb = new StringBuilder();
			}else{
				sb.append(row.charAt(i));
			}
		}
		entries.add(sb.toString().trim());	
		return entries.toArray(new String[entries.size()]);
	}

}
