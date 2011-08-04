package playground.florian.GTFSConverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;

public class GtfsConverter {

	private String filepath = "";

	private CoordinateTransformation transform;

	private Config config;
	private ScenarioImpl scenario;
	private Map<String,Integer> vehicleIdsAndTypes = new HashMap<String,Integer>();
	private Map<Id,Integer> vehicleTypesToRoutesAssignments = new HashMap<Id,Integer>();
	private boolean createShapedNetwork = false;

	private long date = 0;


	// Fields for shaped Network
	// (TripId,ShapeId)
	private Map<String,String> shapeIdToTripIdAssignments = new HashMap<String,String>();
	// (LinkId,(TripId,FromShapeDist,ToShapeDist))
	private Map<Id,String[]> shapedLinkIds = new HashMap<Id,String[]>();
	// If there is no shape_dist_traveled field, try to identify the stations by its coordinates
	// (LinkId,(TripId,FromCoordX, FromCoordY ,ToCoordX, ToCoordY)) - Both Coordinates as Strings and in Matsim-KS
	private Map<Id,String[]> shapedLinkIdsCoordinate = new HashMap<Id,String[]>();
	private boolean alternativeStationToShapeAssignment = false;
	private double toleranceInM = 0;
	// (ShapeId, (shapeDist, x, y))
	private Map<Id,List<String[]>> shapes = new HashMap<Id,List<String[]>>();



	private TransitSchedule ts;


	public GtfsConverter(String filepath, CoordinateTransformation transform) {
		this.filepath = filepath;
		this.transform = transform;
	}

	private void createConfig(){
		config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		QSimConfigGroup qsim = new QSimConfigGroup();
		//		qsim.setStartTime(Time.parseTime("00:00:00"));
		//		qsim.setEndTime(Time.parseTime("17:00:00"));
		config.addQSimConfigGroup(qsim);
		Set<ControlerConfigGroup.EventsFileFormat> eventsFileFormats = new HashSet<ControlerConfigGroup.EventsFileFormat>();
		eventsFileFormats.add(ControlerConfigGroup.EventsFileFormat.xml);
		config.controler().setEventsFileFormats(eventsFileFormats);		
		Set<String> transitModes = new HashSet<String>();
		transitModes.add("pt");
		config.transit().setTransitModes(transitModes);
		config.transit().setVehiclesFile("transitVehicles.xml");
		config.transit().setTransitScheduleFile("../playgrounds/florian/transitSchedule.xml");
		config.network().setInputFile("../playgrounds/florian/network.xml");
	}


	private void writeConfig() {
		ConfigWriter cw = new ConfigWriter(config);
		cw.setPrettyPrint(true);
		cw.write("./config.xml");
	}

	@SuppressWarnings("deprecation")
	public void convert(){		
		// Create a config
		this.createConfig();		
		this.scenario = (ScenarioImpl)(ScenarioUtils.createScenario(config));

		ts = scenario.getTransitSchedule();

		// Put all stops in the Schedule
		this.convertStops();

		// Get the Routenames and the assigned Trips
		
		Map<Id,String> routeNames = getRouteNames(filepath + "/routes.txt");
		
		Map<Id,Id> routeToTripAssignments = getRouteToTripAssignments(filepath + "/trips.txt");

		// Create Transitlines
		this.createTransitLines(routeNames);

		// Get the used service Id for the choosen weekday and date
		List<String> usedServiceIds = new ArrayList<String>();
		String calendarFilename = filepath + "/calendar.txt";
		if((new File(calendarFilename)).exists()){
			System.out.println("Reading calendar.txt");
			usedServiceIds.addAll(this.getUsedServiceIds(calendarFilename));
		}
		String calendarDatesFilename = filepath + "/calendar_dates.txt";
		if((new File(calendarDatesFilename)).exists()){
			System.out.println("Reading calendar_dates.txt");
			for(String serviceId: this.getUsedServiceIdsForSpecialDates(calendarDatesFilename)){
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
		List<Id> usedTripIds = this.getUsedTripIds(filepath + "/trips.txt", usedServiceIds);
		System.out.println("Reading of TripIds succesfull: " + usedTripIds);
		
		System.out.println("Creating Network");
		String stopTimesFilename = filepath + "/stop_times.txt";
		this.createNetworkOfStopsAndTrips(stopTimesFilename, ts);

		// Get the TripRoutes
		Map<Id,NetworkRoute> tripRoute;
		System.out.println("Create NetworkRoutes");
		tripRoute = createNetworkRoutes(stopTimesFilename);
		if(this.createShapedNetwork){
			System.out.println("Creating shaped Network");
			this.convertShapes();
			Map<Id, List<Coord>> shapedLinks = this.assignShapesToLinks();
			NetworkEnricher networkEnricher = new NetworkEnricher(scenario.getNetwork());
			tripRoute = networkEnricher.replaceLinks(shapedLinks, tripRoute);
			scenario.setNetwork(networkEnricher.getEnrichedNetwork());
		}
		
		// Convert the schedules for the trips
		System.out.println("Convert the schedules");
		this.convertSchedules(stopTimesFilename, routeNames, routeToTripAssignments, usedTripIds, tripRoute);

		// If you use the optional frequencies.txt, it will be transformed here
		String frequenciesFilename = filepath + "/frequencies.txt";
		if((new File(frequenciesFilename)).exists()){
			this.convertFrequencies(frequenciesFilename, routeToTripAssignments, usedTripIds);
		}

		// Create some dummy Vehicles
		this.createTransitVehiclesDummy();

		if(usedTripIds.isEmpty()){
			System.out.println("There are no converted trips. You might need to change the date for better results.");
		}
		System.out.println("Conversion successfull");
	}


	public void writeScenario() {
		writeConfig();
		writeNetwork();
		writeTransitSchedule();
		writeVehicles();
	}



	private void writeNetwork() {
		new NetworkWriter(scenario.getNetwork()).write("./network.xml");
		System.out.println("Wrote Network to " + new File("./network.xml").getAbsolutePath());
	}


	private void writeTransitSchedule() {
		System.out.println("Writing TransitSchedule.");
		TransitScheduleWriter tsw = new TransitScheduleWriter(ts);
		tsw.writeFile("./transitSchedule.xml");
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

	private List<Id> getUsedTripIds(String tripsFilename, List<String> usedServiceIds) {
		List<Id> usedTripIds = new ArrayList<Id>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(tripsFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
			int serviceIdIndex = header.indexOf("service_id");
			int tripIdIndex = header.indexOf("trip_id");
			int shapeIdIndex = header.indexOf("shape_id");
			String row = br.readLine();
			do {
				String[] entries = GtfsConverter.splitRow(row);
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

	private Map<Id,String> getRouteNames(String routesFilename) {
		Map<Id,String> routeNames = new HashMap<Id,String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(routesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(splitRow(br.readLine())));
			int routeIdIndex = header.indexOf("route_id");
			int routeLongNameIndex = header.indexOf("route_long_name");
			int vehicleTypeIndex = header.indexOf("route_type");
			String row = br.readLine();
			do {
				String[] entries = splitRow(row);
				routeNames.put(new IdImpl(entries[routeIdIndex]), entries[routeLongNameIndex]);
				this.vehicleTypesToRoutesAssignments.put(new IdImpl(entries[routeIdIndex]), Integer.parseInt(entries[vehicleTypeIndex].trim()));
				row = br.readLine();
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(routesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return routeNames;
	}

	private Map<Id,Id> getRouteToTripAssignments(String tripsFilename){
		Map<Id,Id> routeTripAssignment = new HashMap<Id,Id>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(tripsFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(splitRow(br.readLine())));
			int routeIdIndex = header.indexOf("route_id");
			int tripIdIndex = header.indexOf("trip_id");
			String row = br.readLine();
			do {
				String[] entries = splitRow(row);
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

	private List<String> getUsedServiceIds(String calendarFilename) {
		List<String> serviceIds = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(calendarFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
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
				String[] entries = GtfsConverter.splitRow(row);
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

	private List<String> getUsedServiceIdsForSpecialDates(String calendarFilename){
		List<String> serviceIds = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(calendarFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
			int serviceIdIndex = header.indexOf("service_id");
			int dateIndex = header.indexOf("date");
			int exceptionTypeIndex = header.indexOf("exception_type");
			String row = br.readLine();
			if(row!= null){
				do {
					String[] entries = GtfsConverter.splitRow(row);
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

	private void convertFrequencies(String frequenciesFilename, Map<Id, Id> routeToTripAssignments, List<Id> usedTripIds) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(frequenciesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int startTimeIndex = header.indexOf("start_time");
			int endTimeIndex = header.indexOf("end_time");
			int stepIndex = header.indexOf("headway_secs");
			String row = br.readLine();
			int departureCounter = 2;
			String oldTripId = "";
			if(row != null){
				do {
					String[] entries = GtfsConverter.splitRow(row);
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
								Departure d = ts.getFactory().createDeparture(new IdImpl(tripId.toString() + "." + departureCounter), time);
								d.setVehicleId(new IdImpl(tripId.toString() + "." + departureCounter));
								this.vehicleIdsAndTypes.put(tripId.toString() + "." + departureCounter,this.vehicleTypesToRoutesAssignments.get(routeToTripAssignments.get(tripId)));
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


	private void convertSchedules(String stopTimesFilename, Map<Id, String> routeNames, Map<Id, Id> routeToTripAssignments, List<Id> tripIds, Map<Id, NetworkRoute> tripRoute){
		List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();
		try {
			int idCounter = 0;
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int arrivalTimeIndex = header.indexOf("arrival_time");
			int departureTimeIndex = header.indexOf("departure_time");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			String[] entries = GtfsConverter.splitRow(row);
			String currentTrip = entries[tripIdIndex];
			Double departureTime = Time.parseTime(entries[arrivalTimeIndex].trim());
			Departure departure = ts.getFactory().createDeparture(new IdImpl(entries[tripIdIndex] + "." + idCounter), departureTime);
			String vehicleId = entries[tripIdIndex] + "." + idCounter;
			departure.setVehicleId(new IdImpl(vehicleId));		
			this.vehicleIdsAndTypes.put(vehicleId,vehicleTypesToRoutesAssignments.get(routeToTripAssignments.get(new IdImpl(entries[tripIdIndex]))));
			do {				
				entries = GtfsConverter.splitRow(row);
				Id currentTripId = new IdImpl(currentTrip);
				Id tripId = new IdImpl(entries[tripIdIndex]);
				Id stopId = new IdImpl(entries[stopIdIndex]);
				if(entries[tripIdIndex].equals(currentTrip)){
					if(tripIds.contains(tripId)){
						TransitStopFacility stop = ts.getFacilities().get(stopId);
						TransitRouteStop routeStop = ts.getFactory().createTransitRouteStop(stop, Time.parseTime(entries[arrivalTimeIndex].trim())-departureTime, Time.parseTime(entries[departureTimeIndex].trim())-departureTime);
						routeStop.setAwaitDepartureTime(true);
						stops.add(routeStop);		
					}
				}else{
					if(tripIds.contains(currentTripId)){
						//finish old route
						TransitLine tl = ts.getTransitLines().get(routeToTripAssignments.get(currentTripId));
						TransitRoute tr = ts.getFactory().createTransitRoute(currentTripId, tripRoute.get(currentTripId), stops, "pt");
						tr.addDeparture(departure);
						tl.addRoute(tr);
						stops = new LinkedList<TransitRouteStop>();
					}
					if(tripIds.contains(tripId)){
						//begin new route
						departureTime = Time.parseTime(entries[arrivalTimeIndex].trim());
						departure = ts.getFactory().createDeparture(new IdImpl(entries[tripIdIndex] + "." + idCounter), departureTime);
						vehicleId = tripId.toString() + "." + idCounter;
						departure.setVehicleId(new IdImpl(vehicleId));
						this.vehicleIdsAndTypes.put(vehicleId,vehicleTypesToRoutesAssignments.get(routeToTripAssignments.get(tripId)));												
						TransitStopFacility stop = ts.getFacilities().get(stopId);
						TransitRouteStop routeStop = ts.getFactory().createTransitRouteStop(stop, 0, Time.parseTime(entries[departureTimeIndex].trim())-departureTime);
						stops.add(routeStop);
					}
					currentTrip = entries[tripIdIndex];						
				}						
				row = br.readLine();
			}while(row!= null);
			// The last trip of the file was not added, so it needs to be added now
			if(tripIds.contains(new IdImpl(currentTrip))){
				Id currentTripId = new IdImpl(currentTrip);
				//finish old route
				TransitLine tl = ts.getTransitLines().get(routeToTripAssignments.get(currentTripId));
				TransitRoute tr = ts.getFactory().createTransitRoute(currentTripId, tripRoute.get(currentTripId), stops, "pt");
				tr.addDeparture(departure);
				tl.addRoute(tr);
			}
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
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
			int shapeIdIndex = header.indexOf("shape_id");
			int shapeLatIndex = header.indexOf("shape_pt_lat");
			int shapeLonIndex = header.indexOf("shape_pt_lon");
			int shapeDistIndex = header.indexOf("shape_dist_traveled");
			if((shapeDistIndex<0) && this.createShapedNetwork){
				this.alternativeStationToShapeAssignment = true;
				System.out.println("Couldn't find the shape_dist_traveled field in shapes.txt. Now it uses the alternative station to shape assingment.");
			}
			String row = br.readLine();
			String[] entries = GtfsConverter.splitRow(row);
			String oldShapeId = entries[shapeIdIndex];
			List<String[]> shapes = new ArrayList<String[]>();
			do {
				entries = GtfsConverter.splitRow(row);
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



	private void convertStops(){
		String filename = filepath + "/stops.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine().trim())));
			int stopIdIndex = header.indexOf("stop_id");
			int stopNameIndex = header.indexOf("stop_name");
			int stopLatitudeIndex = header.indexOf("stop_lat");
			int stopLongitudeIndex = header.indexOf("stop_lon");
			String row = br.readLine();
			do{
				String[] entries = GtfsConverter.splitRow(row);
				TransitStopFacility t;
				t = this.ts.getFactory().createTransitStopFacility(new IdImpl(entries[stopIdIndex]), transform.transform(new CoordImpl(Double.parseDouble(entries[stopLongitudeIndex]), Double.parseDouble(entries[stopLatitudeIndex]))), false);
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

	private Map<Id,NetworkRoute> createNetworkRoutes(String stopTimesFilename) {
		Map<Id,NetworkRoute> tripRoutes = new HashMap<Id,NetworkRoute>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
			int tripIdIndex = header.indexOf("trip_id");
			int stopIdIndex = header.indexOf("stop_id");
			String row = br.readLine();
			String[] entries = row.split(",");			
			LinkedList<Id> route = new LinkedList<Id>();
			String currentTrip = entries[tripIdIndex];
			String startStation = entries[stopIdIndex];
			do {
				entries = GtfsConverter.splitRow(row);
				String nextStation = entries[stopIdIndex];				
				if(currentTrip.equals(entries[tripIdIndex])){					
					if(!(startStation.equals(nextStation))){
						IdImpl startStationNodeId = new IdImpl(startStation);
						IdImpl nextStationNodeId = new IdImpl(nextStation);
						Id linkId = findLinkFromNodeToNode(startStationNodeId, nextStationNodeId);
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
					// Start new Route
					currentTrip = entries[tripIdIndex];
					route = new LinkedList<Id>();
					route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
					startStation = entries[stopIdIndex];
				}
				row = br.readLine();
			}while(row!= null);		
			NetworkRoute netRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getFirst(), route.getLast());
			if(route.size() > 2){
				netRoute.setLinkIds(route.getFirst(), route.subList(1, route.size()-1), route.getLast());
			}
			tripRoutes.put(new IdImpl(currentTrip), netRoute);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tripRoutes;		
	}


	private Id findLinkFromNodeToNode(IdImpl fromNodeId, IdImpl toNodeId) {
		Id linkId = null;
		for(Id fromId: scenario.getNetwork().getNodes().get(fromNodeId).getOutLinks().keySet()){
			for(Id toId: scenario.getNetwork().getNodes().get(toNodeId).getInLinks().keySet()){
				if(fromId.equals(toId)){
					linkId = fromId;
				}
			}
		}
		return linkId;
	}

	private void createTransitLines(Map<Id, String> routeNames) {
		for(Id id: routeNames.keySet()){
			TransitLine tl = ts.getFactory().createTransitLine(id);
			ts.addTransitLine(tl);
		}		
	}

	private void createNetworkOfStopsAndTrips(String stopTimesFilename, TransitSchedule ts){
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
		Map<Id, Node> nodes = network.getNodes();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(stopTimesFilename)));
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(br.readLine())));
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
				String[] entries = GtfsConverter.splitRow(row);
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
					entries = GtfsConverter.splitRow(row);
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
					}									
				}		
			}while(row!= null);
		} catch (FileNotFoundException e) {
			System.out.println(stopTimesFilename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void createTransitVehiclesDummy(){
		for(String s: vehicleIdsAndTypes.keySet()){
			// TYPE
			VehicleType vt;
			switch(vehicleIdsAndTypes.get(s)){
				case 0: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Tram"));break;
				case 1: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Subway"));break;
				case 2: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Rail"));break;
				case 3: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Bus"));break;
				case 4: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Ferry"));break;
				case 5: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_CableCar"));break;
				case 6: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Gondola"));break;
				case 7: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_Funicular"));break;
				default: vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("dummy_UnidentifiedType"));
			}
			vt.setDescription("Dummy Vehicle Type for GTFS Converter. The Id gives Information about the GTFS-Type. Please change the following parameters for fitting your purposes.");
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			vt.setCapacity(vc);
			vt.setLength(5);
			scenario.getVehicles().getVehicleTypes().put(vt.getId(), vt);
			// Vehicle
			Vehicle v = scenario.getVehicles().getFactory().createVehicle(new IdImpl(s), vt);
			scenario.getVehicles().getVehicles().put(new IdImpl(s), v);
		}
	}


	private void writeVehicles() {
		VehicleWriterV1 vw = new VehicleWriterV1(scenario.getVehicles());
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
	
	public static void convertStopTimesToAcceptedFormat(String stopTimesPath){
		Map<String,List<String>> rowToTripAssignments = new HashMap<String,List<String>>();
		File f = new File(stopTimesPath);
		String headerText = "error";
		// READ FILE
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			headerText = br.readLine();
			List<String> header = new ArrayList<String>(Arrays.asList(GtfsConverter.splitRow(headerText)));
			int tripIdIndex = header.indexOf("trip_id");
			int stopSequenceIndex = header.indexOf("stop_sequence");
			String row = br.readLine();
			do{
				String[] entries = GtfsConverter.splitRow(row);
				String tripId = entries[tripIdIndex];
				Integer stopSequence = Integer.parseInt(entries[stopSequenceIndex]);
				if(!rowToTripAssignments.containsKey(tripId)){
					rowToTripAssignments.put(tripId, new ArrayList<String>());
				}
				if(stopSequence < rowToTripAssignments.get(tripId).size()){
					rowToTripAssignments.get(tripId).set(stopSequence, row);
				}else{
					for(int i=rowToTripAssignments.get(tripId).size(); i<=stopSequence; i++){
						rowToTripAssignments.get(tripId).add("");
					}
					rowToTripAssignments.get(tripId).set(stopSequence, row);
				}
				row = br.readLine();
			}while(row != null);
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't find " + f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		// WRITE FILE
		File of = new File(stopTimesPath + "_b");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(of));
			bw.write(headerText + "\n");
			for(String tripId: rowToTripAssignments.keySet()){
				for(String row: rowToTripAssignments.get(tripId)){
					if(!row.equals("")){
						bw.write(row + "\n");
					}					
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println("Couldn't write to " + of.getAbsolutePath());
		}
	}


	private static String[] splitRow(String row){
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

	public Scenario getScenario() {
		return scenario;
	}

}
