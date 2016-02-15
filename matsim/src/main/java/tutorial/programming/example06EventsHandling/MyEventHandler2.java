package tutorial.programming.example06EventsHandling;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * Actually, handling Departures and Arrivals should be sufficient for this (may 2014)
 * @author dgrether
 *
 */
public class MyEventHandler2 implements 
	PersonArrivalEventHandler,
	PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private double timePersonOnTravel = 0.0;
	private double timeVehicleInTraffic = 0.0 ;
	// NOTE: drivers depart, enter vehicles, eventually enter traffic, drive to destination, leave traffic, leave vehicle, arrive.
	// In consequence, the time between departure and arrival of the person may be longer than the time
	// between the vehicle entering and leaving the traffic (network).
	
	public double getTotalTravelTime() {
		return this.timePersonOnTravel ;
	}
	public double getTotalVehicleInTrafficTime() {
		return this.timeVehicleInTraffic ;
	}

	@Override
	public void reset(int iteration) {
		this.timePersonOnTravel = 0.0;
		this.timeVehicleInTraffic = 0.0 ;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.timePersonOnTravel += event.getTime();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.timePersonOnTravel -= event.getTime();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.timeVehicleInTraffic += event.getTime() ;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.timeVehicleInTraffic -= event.getTime() ;
	}
}
