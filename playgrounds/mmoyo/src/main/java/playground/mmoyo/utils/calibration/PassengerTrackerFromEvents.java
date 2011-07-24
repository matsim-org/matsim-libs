package playground.mmoyo.utils.calibration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.counts.OccupancyAnalyzer;

import playground.mmoyo.io.TextFileWriter;


/** identifies passengers who traveled along a stop reading an event file*/
public class PassengerTrackerFromEvents implements PersonEntersVehicleEventHandler,
											PersonLeavesVehicleEventHandler, 
											VehicleArrivesAtFacilityEventHandler,
											VehicleDepartsAtFacilityEventHandler{

	private static final Logger log = Logger.getLogger(PassengerTrackerFromEvents.class);
	private final Map<Id, Id> veh_stops = new HashMap<Id, Id>();
	private final Map<Id, List<Id>> veh_passengers = new HashMap<Id, List<Id>>();
	public final Map<Id, List<Id>> stop_passengers = new HashMap<Id, List<Id>>();
	public List<Tracker> trackerList = new ArrayList<Tracker>();
	private List<Id> stopIdList;
	private String RouteId;
	private double binTime=-1;
	
	public PassengerTrackerFromEvents(List<Id> stopIdList, String RouteId, final int bin) {
		log.setLevel( Level.INFO ) ;
		this.stopIdList = stopIdList;
		this.RouteId = RouteId;
		this.binTime= (bin-1)*3600;
	}
	
	@Override
	public void reset(int iteration) {
		this.veh_stops.clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id transitRouteId = ((PersonEntersVehicleEventImpl) event).getTransitRouteId() ;
		if (!transitRouteId.toString().contains(this.RouteId)) {
			return ;
		}
		
		Id vehId = event.getVehicleId(); 
		if (this.veh_passengers.get(vehId)==null){
			this.veh_passengers.put(vehId, new ArrayList<Id>());
		}
		this.veh_passengers.get(vehId).add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id transitRouteId = ((PersonLeavesVehicleEventImpl) event).getTransitRouteId() ;
		if ( !transitRouteId.toString().contains(this.RouteId)) {
			return ;
		}
		
		
		Id vehId = event.getVehicleId();
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
		Id stopId = event.getFacilityId();
		this.veh_stops.put(event.getVehicleId(), stopId);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id stopId = event.getFacilityId();
		Id vehId = event.getVehicleId();
		this.veh_stops.remove(vehId);

		if (stopIdList.contains(stopId)){ //and veh belongs to route?
		
			if (this.veh_passengers.get(vehId)==null){
				this.veh_passengers.put(vehId, new ArrayList<Id>());
			}
			
			if (stop_passengers.get(stopId)==null){
				stop_passengers.put(stopId, new ArrayList<Id>());
			}
			
			//add all passengers of the bus to the list of stop-passenger list
			for (Id passengerId: this.veh_passengers.get(vehId)){
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
		Id stationId;
		double time;
		Id personId;

		Tracker(Id stationId, double time, Id person){
			this.stationId = stationId;
			this.time = time;
			this.personId= person;
		}
		
	}

	public static void main(String[] args) {
		String eventFilePath = "../../input/juni/calibration/500/500.events.xml.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
	
		List<Id> stopList = new ArrayList<Id>();
		stopList.add(new IdImpl("812020.3"));
		stopList.add(new IdImpl("812550.1"));
		stopList.add(new IdImpl("812030.1"));
		stopList.add(new IdImpl("812560.1"));
		stopList.add(new IdImpl("812570.1"));
		stopList.add(new IdImpl("813013.1"));
		stopList.add(new IdImpl("806520.1"));
		stopList.add(new IdImpl("806030.1"));
		stopList.add(new IdImpl("806010.1"));
		stopList.add(new IdImpl("806540.1"));
		stopList.add(new IdImpl("804070.3"));
		stopList.add(new IdImpl("804060.2"));
		stopList.add(new IdImpl("801020.1"));
		stopList.add(new IdImpl("801030.3"));
		stopList.add(new IdImpl("801530.1"));
		stopList.add(new IdImpl("801040.1"));
		stopList.add(new IdImpl("792050.3"));
		
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