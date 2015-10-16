package playground.toronto.gtfsutils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;

import playground.toronto.demand.util.TableReader;
import GTFS2PTSchedule.Frequency;
import GTFS2PTSchedule.GTFSDefinitions.RouteTypes;
import GTFS2PTSchedule.Route;
import GTFS2PTSchedule.Service;
import GTFS2PTSchedule.Shape;
import GTFS2PTSchedule.Stop;
import GTFS2PTSchedule.StopTime;
import GTFS2PTSchedule.Trip;

/**
 * Stores all of the data of a set of GTFS files. Provides functions for automatically loading all the available data,
 * as well as getters of the data. 
 * 
 * @author pkucirek
 *
 */
public class GTFSSystem {

	private static final Logger log = Logger.getLogger(GTFSSystem.class);
	
	private static final List<String> stopsFileHeaders = Arrays.asList(new String[]
			{"stop_id",
			"stop_code",
			"stop_name",
			"stop_desc",
			"stop_lat",
			"stop_lon",
			"zone_id",
			"stop_url",
			"location_type",
			"parent_station"});
	private static final List<String> tripsFileHeaders = Arrays.asList(new String[]
			{"route_id",
			"service_id",
			"trip_id",
			"trip_headsign",
			"direction_id",
			"shape_id"});
	private static final List<String> stopTimesFileHeaders = Arrays.asList(new String[]
			{"trip_id",
			"arrival_time",
			"departure_time",
			"stop_id",
			"stop_sequence",
			"stop_headsign",
			"pickup_type",
			"drop_off_type"});
	private static final List<String> routesFileHeaders = Arrays.asList(new String[]
			{"route_id",
			"agency_id",
			"route_short_name",
			"route_long_name",
			"route_desc",
			"route_type",
			"route_url",
			"route_color",
			"route_text_color"});
	private static final List<String> calendarFileHeaders = Arrays.asList(new String[]
			{"service_id",
			"monday",
			"tuesday",
			"wednesday",
			"thursday",
			"friday",
			"saturday",
			"sunday",
			"start_date",
			"end_date"});
	private static final List<String> agencyHeaders = Arrays.asList(new String[]
			{"agency_id",
			"agency_name",
			"agency_url",
			"agency_timezone",
			"agency_lang",
			"agency_phone"});
	private static final List<String> frequenciesFileHeaders = Arrays.asList(new String[]
			{"trip_id",
			"start_time",
			"end_time",
			"headway_secs",
			"exact_times"});
	private static final List<String> shapesFileHeaders = Arrays.asList(new String[]
			{"shape_id",
			"shape_pt_lat",
			"shape_pt_lon",
			"shape_pt_sequence",
			"shape_dist_traveled"});

	private TreeMap<Id<Stop>, Stop> stops;
	private TreeMap<Id<Route>, Route> routes;
	private TreeMap<Id<Service>, Service> services;
	private TreeMap<Id<Shape>, Shape> shapes;
	private TreeMap<Id<Frequency>, Frequency> frequencies;
	private HashMap<String, Route> tripToRoute; //tripId -> routeId
	
	//CONSTRUCTOR--------------------------------------------------------------------------------------------------------------------------
	
	public GTFSSystem(){
		this.stops = new TreeMap<>();
		this.routes = new TreeMap<>();
		this.services = new TreeMap<>();
		this.shapes = new TreeMap<>();
		this.tripToRoute = new HashMap<String, Route>();
	}
	
	//INTERNAL METHODS----------------------------------------------------------------------------------------------------------------------
	
		private void loadStops(String fileName) throws FileNotFoundException, IOException{
			TableReader tr = new TableReader(fileName, ",");
			tr.open();
			if (!tr.checkHeaders(stopsFileHeaders)) throw new IOException("Stops file is incorrectly formatted!");
			tr.ignoreTrailingBlanks(true);
			
			this.stops.clear();
			while (tr.next()){
				Id<Stop> stopId = Id.create(tr.current().get("stop_id"), Stop.class);
				Coord coord = new Coord(Double.parseDouble(tr.current().get("stop_lon")), Double.parseDouble(tr.current().get("stop_lat")));
				
				Stop s = new Stop(coord, tr.current().get("stop_name"), false);
				this.stops.put(stopId, s);
			}
			tr.close();
		}
		
		private void loadCalendar(String filename) throws FileNotFoundException, IOException{
			TableReader tr = new TableReader(filename, ",");
			tr.open();
			if (!tr.checkHeaders(calendarFileHeaders)) throw new IOException("Calendar file is incorrectly formatted!");
			
			this.services.clear();
			while (tr.next()){
				Id<Service> id = Id.create(tr.current().get("service_id"), Service.class);
				boolean[] days = new boolean[7];
				for (int i = 1; i < 8; i++){
					String day = "";
					switch (i){
					case 1: day = "monday"; break;
					case 2: day = "tuesday"; break;
					case 3: day = "wednesday"; break;
					case 4: day = "thursday"; break;
					case 5: day = "friday"; break;
					case 6: day = "saturday"; break;
					case 7: day = "sunday"; break;
					}
					
					int avail = Integer.parseInt(tr.current().get(day));
					days[i - 1] = (avail != 0);
				}
				
				Service s = new Service(days, tr.current().get("start_date"), tr.current().get("end_date"));
				this.services.put(id, s);
			}
			tr.close();
		}
		
		private void loadRoutes(String filename) throws FileNotFoundException, IOException{
			TableReader tr = new TableReader(filename, ",");
			tr.open();
			if (!tr.checkHeaders(routesFileHeaders)) throw new IOException("Routes file is incorrectly formatted!");
			
			this.routes.clear();
			while (tr.next()){
				Id<Route> routeId = Id.create(tr.current().get("route_id"), Route.class);
				Route r = new Route(tr.current().get("route_short_name"), getWayType(Integer.parseInt(tr.current().get("route_type")))); 
				this.routes.put(routeId, r);
			}
			tr.close();
		}
		
		private void loadTrips(String fileName) throws FileNotFoundException, IOException{
			TableReader tr = new TableReader(fileName, ",");
			tr.open();
			if (!tr.checkHeaders(tripsFileHeaders)) throw new IOException("Trips file is incorrectly formatted!");
			
			while (tr.next()){
				String tripId = tr.current().get("trip_id");
				
				Id<Route> routeId = Id.create(tr.current().get("route_id"), Route.class);
				Route r = this.routes.get(routeId);
				if (r == null) throw new IOException("Cannot find a route for trip " + tripId + "!"); //this cannot be null
				
				Id<Service> serviceId = Id.create(tr.current().get("service_id"), Service.class);
				Service service = this.services.get(serviceId);
				if (service == null) throw new IOException("Cannot find a service for trip " + tripId + "!"); //this cannot be null
				
				Id<Shape> shapeId = Id.create(tr.current().get("shape_id"), Shape.class);
				Shape shape = this.shapes.get(shapeId); //This can be null.
				
				Trip t = new Trip(service, shape, tr.current().get("trip_headsign"));
				this.tripToRoute.put(tripId, r);
				r.putTrip(tripId, t);
			}
			tr.close();
		}
		
		private void loadStopTimes(String filename) throws FileNotFoundException, IOException{
			TableReader tr = new TableReader(filename, ",");
			tr.open();
			if (!tr.checkHeaders(stopTimesFileHeaders)) throw new IOException("Stop times file is incorrectly formatted!");
			tr.ignoreTrailingBlanks(true);
			
			while (tr.next()){
				String tripId = tr.current().get("trip_id");
				Route r = this.tripToRoute.get(tripId);
				if (r == null) throw new IOException("Cannot get route!");
				
				Trip t = r.getTrips().get(tripId);
				if (t == null) throw new IOException("Cannot get trip!");
				
				double arrivalTime = Time.parseTime(tr.current().get("arrival_time"));
				Date arrivalDate = new Date();
				arrivalDate.setTime((int)(arrivalTime * 1000));
				double departureTime = Time.parseTime(tr.current().get("departure_time"));
				Date departureDate = new Date();
				departureDate.setTime((int)(departureTime * 1000));
				
				int seq = Integer.parseInt(tr.current().get("stop_sequence"));
				StopTime st = new StopTime(arrivalDate, departureDate, tr.current().get("stop_id"));
				
				t.putStopTime(seq, st);				
			}
			tr.close();
			//this.tripToRoute.clear(); //No longer needed. Clear to free up memory.
		}
		
		private void loadShapes(String filename){
			//TODO implement shapes file loading.
		}
		
		private void loadFrequencies(String filename) throws FileNotFoundException, IOException{
			TableReader tr = new TableReader(filename, ",");
			tr.open();
			if (!tr.checkHeaders(frequenciesFileHeaders)) throw new IOException("Frequencies file is incorrectly formatted!");
			
			while (tr.next()){
				int headway = Integer.parseInt(tr.current().get("headway_secs"));
				Date startTime = new Date();
				startTime.setTime((int)(Time.parseTime(tr.current().get("start_time")) * 1000));
				Date endTime = new Date();
				endTime.setTime((int)(Time.parseTime(tr.current().get("end_time")) * 1000));
				
				Frequency f = new Frequency(startTime, endTime, headway);
				
				Route route = this.tripToRoute.get(tr.current().get("trip_id"));
				Trip trip = route.getTrips().get(tr.current().get("trip_id"));
				trip.addFrequency(f);
			}
			
			tr.close();
		}

		//PUBLIC METHODS-----------------------------------------------------------------------------------------------------------------------
		
		public void loadGTFSfiles(String folderName) throws IOException{		
			log.info("Loading GTFS files from " + folderName);
			
			File calendarFile = new File(folderName + "/calendar.txt");
			File stopsFile = new File(folderName + "/stops.txt");
			File routesFile = new File(folderName + "/routes.txt");
			File tripsFile = new File(folderName + "/trips.txt");
			File stopTimesFile = new File(folderName + "/stop_times.txt");
			File shapesFile = new File(folderName + "/shapes.txt");
			File frequenciesFile = new File(folderName + "/frequencies.txt");
			
			//Check that the required files exist
			if (!calendarFile.exists()) throw new FileNotFoundException("Could not find " + calendarFile.getAbsolutePath());
			if (!stopsFile.exists()) throw new FileNotFoundException("Could not find " + stopsFile.getAbsolutePath());
			if (!routesFile.exists()) throw new FileNotFoundException("Could not find " + routesFile.getAbsolutePath());
			if (!tripsFile.exists()) throw new FileNotFoundException("Could not find " + tripsFile.getAbsolutePath());
			if (!stopTimesFile.exists()) throw new FileNotFoundException("Could not find " + stopTimesFile.getAbsolutePath());
			
			
			//TODO load agency
			
			//load calendar
			loadCalendar(calendarFile.getAbsolutePath());
			log.info("Calendar loaded.");
			
			//load stops
			loadStops(stopsFile.getAbsolutePath());
			log.info("Stops loaded.");
			
			//optional: load shapes
			if (shapesFile.exists()){
				loadShapes(shapesFile.getAbsolutePath());
				log.info("OPTIONAL: Shapes loaded.");
			}
			
			//load routes
			loadRoutes(routesFile.getAbsolutePath());
			log.info("Routes loaded.");
			
			//load trips
			loadTrips(tripsFile.getAbsolutePath());
			log.info("Trips loaded.");
			
			//load stop times
			loadStopTimes(stopTimesFile.getAbsolutePath());
			log.info("Stop times loaded.");
			
			//optional: load frequencies
			if (frequenciesFile.exists()){
				loadFrequencies(frequenciesFile.getAbsolutePath());
				log.info("OPTIONAL: Frequencies loaded.");
			}
			
			//optional: load fare attributes
			//optional: load fare rules
			//optional: load transfer rules
		}
		
		public TreeMap<Id<Stop>, Stop> getStops(){
			return this.stops;
		}
		
		public TreeMap<Id<Route>, Route> getRoutes(){
			return this.routes;
		}
		
		public TreeMap<Id<Frequency>, Frequency> getFrequencies(){
			return this.frequencies;
		}
		
		public Service getService(String id){
			return this.services.get(Id.create(id, Service.class));
		}
		
		private RouteTypes getWayType(int i){
			RouteTypes s = null;
			
			switch (i){
			case 0: s = RouteTypes.TRAM; break;
			case 1: s = RouteTypes.SUBWAY; break;
			case 2: s = RouteTypes.RAIL; break;
			case 3: s = RouteTypes.BUS; break;
			case 4: s = RouteTypes.FERRY; break;
			case 5: s = RouteTypes.CABLE_CAR; break;
			/*case 6: s = "gondola"; break; //For some reason these are not supported in Sergio's code. @pkucirek
			case 7: s = "funicular"; break;*/
			default: throw new IllegalArgumentException("Could not recognize way tpye " + i + "!");
			}
			
			return s;
		}

}
