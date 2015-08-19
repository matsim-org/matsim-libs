package playground.dhosse.gap.scenario;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.Vehicles;

public class TransitScheduleCSVReader {
	
	private final static Logger log = Logger.getLogger(TransitScheduleCSVReader.class); 
	
	private static int warnCounter = 0;
	
	private Scenario scenario;
	private TransitSchedule schedule;
	private Vehicles vehicles;
	
	private final LeastCostPathCalculator router;
	
	private static int vehicleCounter = 0;
	
	public TransitScheduleCSVReader(Scenario scenario){
		
		this.scenario = scenario;
		this.schedule = scenario.getTransitSchedule();
		
		this.router = new Dijkstra(this.scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(-6., 0., 0), new FreespeedTravelTimeAndDisutility(-6., 0., 0));
		
		log.info("Initializing vehicle types...");
		
		this.vehicles = scenario.getTransitVehicles();
		
		VehicleType type = new VehicleTypeImpl(Id.create("bus", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(45); //TODO
		capacity.setStandingRoom(55); //TODO
		type.setCapacity(capacity);
		type.setDoorOperationMode(DoorOperationMode.serial);
		EngineInformation currentEngineInfo = new EngineInformationImpl(FuelType.diesel, 4);
		type.setEngineInformation(currentEngineInfo);
		type.setLength(18);
		type.setMaximumVelocity(80);
		type.setWidth(3);
		
		this.vehicles.addVehicleType(type);
		
	}
	
	public void readFileAndAddLines(String csvFile, String mode){
		
		log.info("Reading transit lines from file " + csvFile  + "...");
		
		BufferedReader reader = IOUtils.getBufferedReader(csvFile);
		
		String line = null;
		
		String[] ids = null;
		Map<String, PtTimeProfile> ptTrips = new HashMap<String, PtTimeProfile>();
		
		Set<String> transitLines = new HashSet<String>();
		
		try {
			
			int i = 0;
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split(",");
				
				int counter = 0;
				String stopName = null;
				
				if(i > 0){
					
					if(parts.length < 1) 
						System.out.println();
					
					stopName = parts[0];

					for(int ii = 1; ii < parts.length; ii++){

						String time = parts[ii].replace(" ", "");
						
						if(!time.equals("")){
							
							try{
								
								double t = Time.parseTime(time);
								
								ptTrips.get(ids[ii - 1]).addRouteStop(stopName, time);
								
							} catch(NumberFormatException e){
								
								if(warnCounter < 5){
									log.warn("Time format " + time + " unknown! No profile point will be created...");
									if(warnCounter == 4){
										log.warn("No more warnings will be displayed...");
									}
								}
								warnCounter++;
								
							}
							
						}
						
					}
					
				} else{
					
					ids = new String[parts.length-1];
					
					try{
					
					for(int ii = 1; ii < parts.length; ii++){
						
						String id = parts[ii] + "_" + counter;
						ids[ii - 1] = id;
						String s = parts[ii].replaceAll("[^0-9.]", "");
						transitLines.add(s);
						
						ptTrips.put(id,new PtTimeProfile(Integer.parseInt(s)));
						counter++;
						
					}
					
					} catch(NumberFormatException e){
						e.printStackTrace();
					}
					
				}
				
				i++;
				
			}
			
			reader.close();
			
			log.info("Done.");
			log.info("Created " + ptTrips.size() + " pt time profiles contained in " + transitLines.size() + " transit lines.");
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		//create classes of equal route profiles
		Map<String, List<String>> routeProfileTypes = new HashMap<>();
		
		for(Entry<String, PtTimeProfile> tpEntry : ptTrips.entrySet()){
			
			PtTimeProfile tp = tpEntry.getValue();
			
			double t0 = Time.parseTime(tp.getRouteProfile().getFirst().getSecond());
			
			double tX = Time.parseTime(tp.getRouteProfile().getLast().getSecond());
			
			StringBuffer stops = new StringBuffer();
			stops.append(tpEntry.getValue().getLineNumber() + "-");
			
			for(Tuple<String,String> tuple : tp.getRouteProfile()){
				stops.append(tuple.getFirst() + "=" + (Time.parseTime(tuple.getSecond()) - t0) + "_");
			}
			
			String hash = stops + "_" + Double.toString(tX - t0) + "sec";
			
			if(!routeProfileTypes.containsKey(hash)){
				
				routeProfileTypes.put(hash, new ArrayList<String>());
				
			}
			
			routeProfileTypes.get(hash).add(tpEntry.getKey());
			
		}
		
		//finally, create transit routes from the route profile classes and add them to the schedule
		for(Entry<String,List<String>> routeTypeEntry : routeProfileTypes.entrySet()){
			
			String id = routeTypeEntry.getKey();
			
			List<TransitRouteStop> stops = new ArrayList<>();
			
			String[] parts = routeTypeEntry.getKey().split("-");
			
			Id<TransitLine> transitLineId = Id.create(parts[0], TransitLine.class);
			
			if(!this.schedule.getTransitLines().containsKey(transitLineId)){
				
				this.schedule.addTransitLine(this.schedule.getFactory().createTransitLine(transitLineId));
				
			}
			
			String[] stopStrings = parts[1].split("_");
			
			for(int i = 0; i < stopStrings.length - 2; i++){
				
				String[] sub = stopStrings[i].split("=");
				String name = sub[0];
				double t = Time.parseTime(sub[1]);
				
				if(name.contains(" ankunft")){
					
					if(!stopStrings[i + 1].equals("")){
						
						String time2 = stopStrings[i + 1].split("=")[1];
						
						if(!time2.equals("") && !time2.equals(null)){
							
							double t2 = Time.parseTime(stopStrings[i + 1].split("=")[1]);
							TransitRouteStop stop = this.schedule.getFactory().createTransitRouteStop(this.schedule.getFacilities().get(Id.create(name.replace(" ankunft", ""), TransitStopFacility.class)), t, t2);
							stops.add(stop);
							i++;
							
						} else{
							
							TransitRouteStop stop = this.schedule.getFactory().createTransitRouteStop(this.schedule.getFacilities().get(Id.create(name.replace(" ankunft", ""), TransitStopFacility.class)), t, t);
							stops.add(stop);
							
						}
						
					} else{
						
						TransitRouteStop stop = this.schedule.getFactory().createTransitRouteStop(this.schedule.getFacilities().get(Id.create(name.replace(" ankunft", ""), TransitStopFacility.class)), t, t);
						stops.add(stop);
						
					}
					
				} else if(name.contains(" abfahrt")){
					
					TransitRouteStop stop = this.schedule.getFactory().createTransitRouteStop(this.schedule.getFacilities().get(Id.create(name.replace(" abfahrt", ""), TransitStopFacility.class)), t, t);
					stops.add(stop);
					
					
				} else{
					
					TransitRouteStop stop = this.schedule.getFactory().createTransitRouteStop(this.schedule.getFacilities().get(Id.create(name, TransitStopFacility.class)), t, t);
					stops.add(stop);
					
				}
				
			}
			
			Id<VehicleType> vehicleType = Id.create(mode, VehicleType.class);
			
			Set<Departure> departures = new HashSet<>();
			int departureCounter = 0;
			
			for(String tpId : routeTypeEntry.getValue()){
				
				PtTimeProfile tp = ptTrips.get(tpId);
				double t0 = Time.parseTime(tp.getRouteProfile().getFirst().getSecond());
				
				Departure dep = this.schedule.getFactory().createDeparture(Id.create(departureCounter, Departure.class), t0);
				
				Id<Vehicle> vehicleId = Id.create(TransitScheduleCSVReader.vehicleCounter, Vehicle.class);
				Vehicle v = new VehicleImpl(vehicleId, this.vehicles.getVehicleTypes().get(vehicleType));
				this.vehicles.addVehicle(v);
				
				dep.setVehicleId(vehicleId);
				departures.add(dep);
				
				departureCounter++;
				TransitScheduleCSVReader.vehicleCounter++;
				
			}
			
			LinkedList<Id<Link>> routeLinkIds = new LinkedList<>();
			
			for(int j = 0; j < stops.size() - 1; j++){
				
				TransitStopFacility stopFacility = stops.get(j).getStopFacility();
				TransitStopFacility nextStopFacility = stops.get(j + 1).getStopFacility();
				
				
				Link link = this.scenario.getNetwork().getLinks().get(stopFacility.getLinkId());
				Link nextLink = this.scenario.getNetwork().getLinks().get(nextStopFacility.getLinkId());
				
				if(j == 0){
					
					routeLinkIds.add(link.getId());
					
				}
				
				if(link != null && nextLink != null){

					Path p = this.router.calcLeastCostPath(link.getToNode(), nextLink.getFromNode(), 0., null, null);
					
					for(Link l : p.links){
						
						routeLinkIds.add(l.getId());
						
					}
					
					routeLinkIds.add(nextLink.getId());
					
				} else{
					
					log.warn("Links with ids " + link.getId() + " and  " + nextLink.getId() + " could not be found...");
					
				}
				
			}
			
			NetworkRoute route = null;
			
			if(routeLinkIds.size() > 0){
				
				route = RouteUtils.createNetworkRoute(routeLinkIds, this.scenario.getNetwork());
				
			}
			
			TransitRoute transitRoute = this.schedule.getFactory().createTransitRoute(Id.create(id, TransitRoute.class), route, stops, TransportMode.pt);
			for(Departure d : departures){
				transitRoute.addDeparture(d);
			}

			this.schedule.getTransitLines().get(transitLineId).addRoute(transitRoute);

		}
		
		log.info("Done creating transit routes from file " + csvFile + "...");
		
	}
	
	class PtTimeProfile{
		
		private final int lineNumber;
		private LinkedList<Tuple<String, String>> routeProfile;
		
		protected PtTimeProfile(int lineNumber){
			
			this.lineNumber = lineNumber;
			this.routeProfile = new LinkedList<>();
			
		}
		
		protected void addRouteStop(String stop, String time){
			
			this.routeProfile.addLast(new Tuple<String, String>(stop, time));
			
		}
		
		protected int getLineNumber(){
			
			return this.lineNumber;
			
		}
		
		protected LinkedList<Tuple<String, String>> getRouteProfile(){
			
			return this.routeProfile;
			
		}
		
	}

}