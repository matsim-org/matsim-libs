package playground.andreas.fixedHeadway;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class FixedHeadwayControler implements VehicleDepartsAtFacilityEventHandler{
	
	private final static Logger log = Logger.getLogger(FixedHeadwayControler.class);
	
	private QSim qSim;
	
	HashMap<Id, FixedHeadwayCycleUmlaufDriver> umlaufDriver;	
	HashMap<Id, FixedHeadwayCycleUmlaufDriver> stopId2LastDriverPassedMap = new HashMap<Id, FixedHeadwayCycleUmlaufDriver>();	

	public FixedHeadwayControler(QSim qSim) {
		super();
		this.qSim = qSim;
//		for (PersonAgent personAgent : ptAgents) {
//			this.umlaufDriver.put(((FixedHeadwayCycleUmlaufDriver) personAgent).getVehicle().getBasicVehicle().getId(), (FixedHeadwayCycleUmlaufDriver) personAgent); 
//		}
//		log.info("initialized with " + this.umlaufDriver.size() + " drivers");
	}

//	@Override
//	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
//		this.handleEvent(new VehicleDepartsAtFacilityEventImpl(event.getTime(), event.getVehicleId(), event.getFacilityId(), event.getDelay()));		
//	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
		if(this.umlaufDriver == null){
			init();
		}
		
		if(event.getDelay() > 180){
			ArrayList<FixedHeadwayCycleUmlaufDriver> vehicleAhead = getVehicleAhead(this.umlaufDriver.get(event.getVehicleId()));
			if(vehicleAhead.size() == 0){
				log.warn(Time.writeTime(event.getTime()) + " - Tried to delay a vehicle at stop, but none is ahead. Vehicle " + event.getVehicleId() + " got delayed by " + event.getDelay() + " at " + event.getFacilityId());	
			} else {
				
				// complex one
//				int numberOfVehicleAhead = vehicleAhead.size();
//				for (int i = 0; i < vehicleAhead.size(); i++) {
//					double delay = numberOfVehicleAhead * event.getDelay() / (vehicleAhead.size() + 1);
//					FixedHeadwayCycleUmlaufDriver veh = vehicleAhead.get(i);
//					veh.setAdditionalDelayAtNextStop(delay);
//					log.info("Vehicle " + veh.getVehicle().getBasicVehicle().getId() + " will be delayed by " + delay + " because vehicle " + event.getVehicleId() + " got delayed by " + event.getDelay() + " seconds.");
//					numberOfVehicleAhead--;
//				}
				
				// simple one
				double delay = event.getDelay() / 2.0;
				FixedHeadwayCycleUmlaufDriver veh = vehicleAhead.get(0);
				veh.setAdditionalDelayAtNextStop(delay);
				log.info(Time.writeTime(event.getTime()) + " - Vehicle " + veh.getVehicle().getVehicle().getId() + " will be delayed by " + delay + " because vehicle " + event.getVehicleId() + " got delayed by " + event.getDelay() + " seconds at " + event.getFacilityId());
			}
		}
		
		this.stopId2LastDriverPassedMap.put(event.getFacilityId(), this.umlaufDriver.get(event.getVehicleId()));
	}

	private void init() {		
		this.umlaufDriver = new HashMap<Id, FixedHeadwayCycleUmlaufDriver>();
		for (PersonAgent personAgent : this.qSim.getTransitAgents()) {
			this.umlaufDriver.put(((FixedHeadwayCycleUmlaufDriver) personAgent).getVehicle().getVehicle().getId(), (FixedHeadwayCycleUmlaufDriver) personAgent); 
		}
		log.info("initialized with " + this.umlaufDriver.size() + " drivers");		
	}

	private ArrayList<FixedHeadwayCycleUmlaufDriver> getVehicleAhead(FixedHeadwayCycleUmlaufDriver driver){
		HashMap<Id, FixedHeadwayCycleUmlaufDriver> nextStop2VehicleMap = new HashMap<Id, FixedHeadwayCycleUmlaufDriver>();
		ArrayList<FixedHeadwayCycleUmlaufDriver> vehicleAhead = new ArrayList<FixedHeadwayCycleUmlaufDriver>();
		
		boolean validStop = false;
		for (TransitRouteStop stop : driver.getTransitRoute().getStops()) {
			if(driver.getNextTransitStop().getId().toString().equalsIgnoreCase(stop.getStopFacility().getId().toString())){
				validStop = true;
			}
			if(validStop){
				if(this.stopId2LastDriverPassedMap.get(stop.getStopFacility().getId()) != null){
					TransitStopFacility nextStopFacility = this.stopId2LastDriverPassedMap.get(stop.getStopFacility().getId()).getNextTransitStop();				
					if(nextStopFacility != null){
						nextStop2VehicleMap.put(nextStopFacility.getId(), this.stopId2LastDriverPassedMap.get(stop.getStopFacility().getId()));
					}
				}				
			}
		}
		
		validStop = false;
		for (TransitRouteStop stop : driver.getTransitRoute().getStops()) {
			if(driver.getNextTransitStop().getId().toString().equalsIgnoreCase(stop.getStopFacility().getId().toString())){
				validStop = true;
			}
			if(validStop){
				if(nextStop2VehicleMap.get(stop.getStopFacility().getId()) != null){
					vehicleAhead.add(nextStop2VehicleMap.get(stop.getStopFacility().getId()));
				}				
			}			
		}
		
		return vehicleAhead;		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub		
	}	

}
