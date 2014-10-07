package playground.mmoyo.utils.calibration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playground.mmoyo.io.TextFileWriter;


/** identifies passengers who traveled along a stop reading an event file*/
public class PassengerTrackerFromEvents implements TransitDriverStartsEventHandler, 
											PersonEntersVehicleEventHandler,
											PersonLeavesVehicleEventHandler, 
											VehicleArrivesAtFacilityEventHandler,
											VehicleDepartsAtFacilityEventHandler{

	private static final Logger log = Logger.getLogger(PassengerTrackerFromEvents.class);
	private final Map<Id<Vehicle>, Id<TransitStopFacility>> veh_stops = new HashMap<>();
	private final Map<Id<Vehicle>, List<Id<Person>>> veh_passengers = new HashMap<>();
	public final Map<Id<TransitStopFacility>, List<Id<Person>>> stop_passengers = new HashMap<>();
	public List<Tracker> trackerList = new ArrayList<Tracker>();
	private List<Id<TransitStopFacility>> stopIdList;
	private String RouteId;
	private double binTime=-1;
	private final Map<Id<Vehicle>, Id<TransitRoute>> vehToRouteId = new HashMap<>();
	
	public PassengerTrackerFromEvents(List<Id<TransitStopFacility>> stopIdList, String RouteId, final int bin) {
		log.setLevel( Level.INFO ) ;
		this.stopIdList = stopIdList;
		this.RouteId = RouteId;
		this.binTime= (bin-1)*3600;
	}
	
	@Override
	public void reset(int iteration) {
		this.veh_stops.clear();
		this.vehToRouteId.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehToRouteId.put(event.getVehicleId(), event.getTransitRouteId());
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id<TransitRoute> transitRouteId = this.vehToRouteId.get(event.getVehicleId());
		if (!transitRouteId.toString().contains(this.RouteId)) {
			return ;
		}
		
		Id<Vehicle> vehId = event.getVehicleId(); 
		if (this.veh_passengers.get(vehId)==null){
			this.veh_passengers.put(vehId, new ArrayList<Id<Person>>());
		}
		this.veh_passengers.get(vehId).add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id<TransitRoute> transitRouteId = this.vehToRouteId.get(event.getVehicleId());
		if ( !transitRouteId.toString().contains(this.RouteId)) {
			return ;
		}
		
		
		Id<Vehicle> vehId = event.getVehicleId();
		int nPassengers = this.veh_passengers.get(vehId).size();
		if (nPassengers == 0) {
			throw new RuntimeException("no passengers in vehicle?");
		}
		this.veh_passengers.get(vehId).remove(event.getPersonId());
		
		if (this.veh_passengers.get(vehId).size() == 0) {
			//this.veh_passengers.remove(vehId);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id<TransitStopFacility> stopId = event.getFacilityId();
		this.veh_stops.put(event.getVehicleId(), stopId);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id<TransitStopFacility> stopId = event.getFacilityId();
		Id<Vehicle> vehId = event.getVehicleId();
		this.veh_stops.remove(vehId);

		if (stopIdList.contains(stopId)){ //and veh belongs to route?
		
			if (this.veh_passengers.get(vehId)==null){
				this.veh_passengers.put(vehId, new ArrayList<Id<Person>>());
			}
			
			if (stop_passengers.get(stopId)==null){
				stop_passengers.put(stopId, new ArrayList<Id<Person>>());
			}
			
			//add all passengers of the bus to the list of stop-passenger list
			for (Id<Person> passengerId: this.veh_passengers.get(vehId)){
				if (binTime>-1){
					double time = event.getTime();
					log.info(time);
					
					if (!(time>this.binTime && time<this.binTime+3600)){
						continue;
					}
					
				}
				stop_passengers.get(stopId).add(passengerId);
			
				trackerList.add(new Tracker(stopId, event.getTime(), passengerId));
			}
		}
	}
	
	class Tracker{
		Id<TransitStopFacility> stationId;
		double time;
		Id<Person> personId;

		Tracker(Id<TransitStopFacility> stationId, double time, Id<Person> person){
			this.stationId = stationId;
			this.time = time;
			this.personId= person;
		}
		
	}

	public static void main(String[] args) {
		String eventFilePath = "../../input/juni/calibration/500/500.events.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();
	
		List<Id<TransitStopFacility>> stopList = new ArrayList<>();
		stopList.add(Id.create("812020.3", TransitStopFacility.class));
		stopList.add(Id.create("812550.1", TransitStopFacility.class));
		stopList.add(Id.create("812030.1", TransitStopFacility.class));
		stopList.add(Id.create("812560.1", TransitStopFacility.class));
		stopList.add(Id.create("812570.1", TransitStopFacility.class));
		stopList.add(Id.create("813013.1", TransitStopFacility.class));
		stopList.add(Id.create("806520.1", TransitStopFacility.class));
		stopList.add(Id.create("806030.1", TransitStopFacility.class));
		stopList.add(Id.create("806010.1", TransitStopFacility.class));
		stopList.add(Id.create("806540.1", TransitStopFacility.class));
		stopList.add(Id.create("804070.3", TransitStopFacility.class));
		stopList.add(Id.create("804060.2", TransitStopFacility.class));
		stopList.add(Id.create("801020.1", TransitStopFacility.class));
		stopList.add(Id.create("801030.3", TransitStopFacility.class));
		stopList.add(Id.create("801530.1", TransitStopFacility.class));
		stopList.add(Id.create("801040.1", TransitStopFacility.class));
		stopList.add(Id.create("792050.3", TransitStopFacility.class));
		
		String RouteId= "B-M44.101.901.H";
		int bin = -1;
		
		PassengerTrackerFromEvents passengerTracker = new PassengerTrackerFromEvents(stopList, RouteId, bin);
		events.addHandler(passengerTracker);
	
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFilePath);
		
		System.out.println("Events file read"); 

		/*
		String SEP = " ";
		List<Id> idList = new ArrayList<Id>();
		for(Entry<Id, List<Id>> entry: tracker.stop_passengers.entrySet() ){
			Id stopId = entry.getKey(); 
			List<Id> passengerList = entry.getValue();
			StringBuffer sBuff = new StringBuffer(stopId.toString());
			
			for(Id passangerId : passengerList){
				sBuff.append(SEP);
				sBuff.append(passangerId);
			
				if (idList.contains(passangerId)){
					//log.info("is already there " + passangerId);
				}else{
					idList.add(passangerId);
					System.out.println (passangerId);
				}
			}
			//log.info(sBuff);
		}
		*/
		
		StringBuffer sBuff = new StringBuffer();
		final String TAB = "\t";
		final String NL = "\n";
		for (Tracker t: passengerTracker.trackerList){
			String track= t.stationId + TAB + t.personId + TAB + t.time + NL;
			System.out.print(track) ;
			sBuff.append(track);
		}
		
		File file = new File(eventFilePath);
		file.getParent();
		
		new TextFileWriter().write(sBuff.toString(), file.getParent() + "/passTracked.txt", false);
		
	}
}