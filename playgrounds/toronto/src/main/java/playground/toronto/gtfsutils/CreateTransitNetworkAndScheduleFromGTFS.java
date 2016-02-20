package playground.toronto.gtfsutils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import GTFS2PTSchedule.Frequency;
import GTFS2PTSchedule.GTFSDefinitions.RouteTypes;
import GTFS2PTSchedule.Route;
import GTFS2PTSchedule.Service;
import GTFS2PTSchedule.Stop;
import GTFS2PTSchedule.StopTime;
import GTFS2PTSchedule.Trip;

/**
 * Creates a simple transit network + schedule from GTFS data. This network, in general, should run <em> as-scheduled</em>!
 * This is useful for comparing with simpler assignment algorithms (e.g. Emme).
 * <br><br>Often, GTFS route services will have store each route departure as a separate trip, in this case it is recommended to
 * run {@link CreateGTFSFrequencies} prior to running this tool.
 * 
 * @author pkucirek
 */
public class CreateTransitNetworkAndScheduleFromGTFS {
	
	private static final Logger log = Logger.getLogger(CreateTransitNetworkAndScheduleFromGTFS.class);
	
	private NetworkImpl network;
	private TransitSchedule schedule;
	private GTFSSystem gtfs;
	private Vehicles vehicles;
	private final CoordinateTransformation converter;
	
	private HashMap<Id<Link>, Set<Id<TransitRoute>>> linkRouteMap;
	
	public CreateTransitNetworkAndScheduleFromGTFS(CoordinateTransformation converter){
		this.converter = converter; //Need to have a coordinate converter to allow for euclidean distance.
	}
	
	private void loadGTFSdata(String foldername) throws IOException{
		this.gtfs = new GTFSSystem();
		this.gtfs.loadGTFSfiles(foldername);
	}
	
	private void processStops(){
		log.info("PROCESSING STOPS");
		
		this.network = NetworkImpl.createNetwork();
		NetworkFactoryImpl netFact = this.network.getFactory();
		TransitScheduleFactory schedFact = new TransitScheduleFactoryImpl();
		this.schedule = schedFact.createTransitSchedule();
		
		int progress = 0;
		Percenter percenter = new Percenter(this.gtfs.getStops().size(), 25);
		String msg = null;
		
		for (Entry<Id<Stop>, Stop> e : this.gtfs.getStops().entrySet()){
			Id<Stop> stopId = e.getKey();
			Stop stop = e.getValue();
			
			//Create node
			Coord coord = this.converter.transform(stop.getPoint());
			Node n = netFact.createNode(Id.create(stopId, Node.class), coord);
			this.network.addNode(n);
			
			//Create loop link at node
			LinkImpl loopLink = (LinkImpl) netFact.createLink(Id.create(stopId +"_LOOP", Link.class), n, n, network, 0.0, 9999, 9999, 1.0);
			loopLink.setType("LOOP");
			this.network.addLink(loopLink);
			
			//Create TransitStop, link it to the loop link
			TransitStopFacility tStop = schedFact.createTransitStopFacility(Id.create(stopId, TransitStopFacility.class), coord, true);
			tStop.setLinkId(loopLink.getId());
			tStop.setName(stop.getName());
			this.schedule.addStopFacility(tStop);
			
			if ((msg = percenter.getMessage(++progress)) != null)
				log.info(msg);
		}
	}
	
	private void processRoutes(Set<Service> services, boolean copyLinks){
		log.info("PROCESSING ROUTES");
		
		NetworkFactoryImpl netFact = this.network.getFactory();
		TransitScheduleFactory schedFact = this.schedule.getFactory();
		
		int links = 0;
		this.linkRouteMap = new HashMap<>();
		
		int progress = 0;
		Percenter percenter = new Percenter(this.gtfs.getRoutes().size(), 10);
		String msg = null;
		
		//Each GTFS route becomes one MATSim line
		for (Entry<Id<Route>, Route> e : this.gtfs.getRoutes().entrySet()){
			Id<Route> lineId = e.getKey();
			Route route = e.getValue();
			
			RouteTypes mode = route.getRouteType();
			
			TransitLine line = schedFact.createTransitLine(Id.create(lineId, TransitLine.class));
			
			//Each GTFS trip becomes one MATSim route
			for (Entry<String, Trip> tpEntry : route.getTrips().entrySet()){
				Trip trip = tpEntry.getValue();
				if (services.contains(trip.getService())){ //filter by specified service period
					
					ArrayList<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>(); //List of stops
					ArrayList<Id<Link>> itinerary = new ArrayList<Id<Link>>(); //List of links
					Id<TransitRoute> routeId = Id.create(tpEntry.getKey(), TransitRoute.class);
					
					//Prepare first stop in trip
					StopTime previousST = trip.getStopTimes().get(1);
					double departure = (int)(previousST.getDepartureTime().getTime() / 1000);
					TransitStopFacility stop = this.schedule.getFacilities().get(Id.create(previousST.getStopId(), TransitStopFacility.class));
					itinerary.add(stop.getLinkId());
					Node fromNode = this.network.getNodes().get(stop.getId());
					
					TransitRouteStop routeStop = schedFact.createTransitRouteStop(stop, 0, 0);
					long firstDepartureTime = previousST.getDepartureTime().getTime();
					routeStops.add(routeStop);
										
					for (int i = 2, q = 0; i <= trip.getStopTimes().size(); i++){
						StopTime st = trip.getStopTimes().get(i + q);
						while (st == null){
							//GTFS specifies that stop indices are in increasing order, but not necessarily with a step size of 1.
							q++;
							st = trip.getStopTimes().get(i + q);
						}						
						stop = this.schedule.getFacilities().get(Id.create(st.getStopId(), TransitStopFacility.class));
						Node toNode = this.network.getNodes().get(stop.getId());
						
						//Create TransitRouteStop
						double departureOffset = (int)((st.getDepartureTime().getTime() - firstDepartureTime) / 1000);
						double arrivalOffset = (int)((st.getArrivalTime().getTime() - firstDepartureTime) / 1000);
						routeStop = schedFact.createTransitRouteStop(stop, arrivalOffset, departureOffset);
						routeStops.add(routeStop);
						
						//Get the stop-to-stop speed
						double stopToStopTime = (int) ((st.getArrivalTime().getTime() - previousST.getDepartureTime().getTime()) / 1000); // in seconds
						double dist = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()); //should be in meters
						double freespeed = dist / stopToStopTime;
						
						if (copyLinks){
							//Have a link for each line
							
							Link l = netFact.createLink(Id.create(links++, Link.class), fromNode, toNode, network, dist, freespeed, 9999, 1.0);
							l.setAllowedModes(Collections.singleton(mode.toString()));
							this.network.addLink(l);
							
							itinerary.add(l.getId());
						}else{
							//Have only ONE link per node-pair per mode.
							
							Id<Link> linkId = Id.create(fromNode.getId().toString() + "-" + toNode.getId().toString() + "_" + mode.toString(), Link.class);
							Link l = this.network.getLinks().get(linkId);
							
							if (l == null){
								//Need a new link
								l = netFact.createLink(linkId, fromNode, toNode, network, dist, freespeed, 9999, 1.0);
								l.setAllowedModes(Collections.singleton(mode.toString()));
								
								HashSet<Id<TransitRoute>> s = new HashSet<>();
								s.add(routeId);
								this.linkRouteMap.put(linkId, s);
								
								/*if (mode.toString().equals("BUS")){
									HashSet<String> modes = new HashSet<String>();
									modes.add(mode.toString());
									modes.add(TransportMode.car);
									l.setAllowedModes(modes);
									
									HashSet<Id> s = new HashSet<Id>();
									s.add(routeId);
									linkRouteMap.put(l.getId(), s);
								}else{
									l.setAllowedModes(Collections.singleton(mode.toString()));
									linkRouteMap.get(l.getId()).add(routeId);
								}*/
								
								this.network.addLink(l);
							}else{
								//Update the speed on the old link if necessary.
								if (freespeed > l.getFreespeed())
									l.setFreespeed(freespeed);
								this.linkRouteMap.get(linkId).add(routeId);
							}
							itinerary.add(l.getId());
						}
						
						itinerary.add(stop.getLinkId());
						
						//Set previous stop and node to current stop and node for next iteration
						previousST = st;
						fromNode = toNode;
					}
					
					int lastIndex = itinerary.size() - 1;
					NetworkRoute netRoute = new LinkNetworkRouteImpl(itinerary.get(0), itinerary.get(lastIndex));
					if (itinerary.size() > 2)
						netRoute.setLinkIds(itinerary.get(0), itinerary.subList(1, lastIndex), itinerary.get(lastIndex));
						
					TransitRoute tRoute = schedFact.createTransitRoute(routeId, netRoute,routeStops,mode.toString());
					tRoute.setDescription(trip.getName());
					
					//Check for departures
					List<Frequency> frequencies = trip.getFrequencies();
					if (frequencies != null){
						if (frequencies.size() > 0){
							int currentDepartureId = 0;
							for (Frequency f : frequencies){
								int interval = f.getSecondsPerDeparture();
								if (interval == 0)
									continue; // skip frequencies with no intervael.
								
								double currentTime = f.getStartTime().getTime() / 1000.0;
								double endTime = f.getEndTime().getTime() / 1000.0;
								
								while (currentTime < endTime){									
									tRoute.addDeparture(schedFact.createDeparture(Id.create(currentDepartureId++, Departure.class), currentTime));
									currentTime += interval;
								}
							}
						}else{
							tRoute.addDeparture(schedFact.createDeparture(Id.create(0, Departure.class), departure));
						}
					}else{
						tRoute.addDeparture(schedFact.createDeparture(Id.create(0, Departure.class), departure));
					}
					
					line.addRoute(tRoute);
				}
			}
			
			//Add line to schedule
			this.schedule.addTransitLine(line);
			
			if ((msg = percenter.getMessage(++progress)) != null)
				log.info(msg);
		}
		
		
	}

	//PUBLIC METHODS---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Runs the conversion process.
	 * 
	 * @param folderName The folder of the GTFS files
	 * @param serviceId A string of the desired service id
	 * @param copyLinks True if you want each transit route to create its own network links, false if you don't want duplicates of links
	 */
	public void run(String folderName, String[] serviceIds, boolean copyLinks) throws IOException{
		
		loadGTFSdata(folderName);
		
		processStops();
		
		HashSet<Service> services = new HashSet<Service>();
		for (String s : serviceIds){
			Service service = this.gtfs.getService(s);
			if (service == null) {
				log.error("Could not find service id " + s + "!");
				continue;
			}
			services.add(service);
		}
		
		processRoutes(services, copyLinks);
		
	}
	
	public void addVehicles(String vehicleTypesFile, String routeVehicleMapFile) throws FileNotFoundException, IOException{
		log.info("ADDING VEHICLE TYPES...");
		
		this.vehicles = VehicleUtils.createVehiclesContainer();
		CreateMultipleVehicleTypesForSchedule vehicleAdder = new CreateMultipleVehicleTypesForSchedule(schedule, vehicles);
		
		vehicleAdder.ReadVehicleTypes(vehicleTypesFile);
		vehicleAdder.ReadRouteVehicleMapping(routeVehicleMapFile);
		vehicleAdder.run();
		
	}
	
	public void exportLinkRouteMap(String filename) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		bw.write("linkId;[route1,route2,...]");
		for (Entry<Id<Link>, Set<Id<TransitRoute>>> e : this.linkRouteMap.entrySet()){
			String s = "[";
			for (Id<TransitRoute> i : e.getValue()){
				s += i.toString() + ",";
			}
			s = s.substring(0, s.length() - 2);
			s += "]";
			bw.write("\n" + e.getKey().toString() + ";" + s);
		}
		bw.close();
	}
	
	public Network getNetwork(){
		return this.network;
	}
	
	public TransitSchedule getSchedule(){
		return this.schedule;
	}
	
	
	//MAIN METHOD---------------------------------------------------------------------------------------------------------------------------
	public static void main(String[] args) throws IOException {
		String folder = args[0];
		String projection = args[1];
		String service = args[2];
		String[] services = service.split(",");
		boolean copyLinks = Boolean.parseBoolean(args[3]);
		String networkFile = args[4];
		String scheduleFile = args[5];
		String vehicleTypesFile = args[6];
		String routeVehiclesFile = args[7];
		String vehiclesFile = args[8];
		
		GeotoolsTransformation converter = new GeotoolsTransformation(TransformationFactory.WGS84, projection);
		
		CreateTransitNetworkAndScheduleFromGTFS bob = new CreateTransitNetworkAndScheduleFromGTFS(converter);
		bob.run(folder, services, copyLinks);
		bob.addVehicles(vehicleTypesFile, routeVehiclesFile);
		
		new NetworkWriter(bob.getNetwork()).write(networkFile);
		new TransitScheduleWriter(bob.getSchedule()).writeFile(scheduleFile);
		new VehicleWriterV1(bob.vehicles).writeFile(vehiclesFile); 
	}
	
	private class Percenter{
		private final int total;
		private int currentPct;
		private final int interval;
		
		private Percenter(int tot, int interval){
			this.total = tot;
			this.interval = interval;
			currentPct = 0;
		}
		
		private String getMessage(int i){
			int pct = i * 100 / this.total; 
			if ((pct % this.interval == 0) && (pct != this.currentPct)){
				this.currentPct = pct;
				return "" + pct + "%";
			}
			
			return null;		
		}
		
	}
	
}
