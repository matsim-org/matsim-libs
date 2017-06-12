/**
 * 
 */
package playground.vsptelematics.bangbang;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * @author kainagel
 *
 */
final class MyTravelTime implements TravelTime, LinkEnterEventHandler, LinkLeaveEventHandler, 
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleArrivesAtFacilityEventHandler, 
VehicleAbortsEventHandler {
	
	TravelTimeCalculator current ;
	TravelTimeCalculator previous = null ;
	
	MyTravelTime(Scenario scenario) {
		current = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator() ) ;
		previous = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator() ) ;
	}


	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return current.toString();
	}

	/**
	 * @param e
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent e) {
		current.handleEvent(e);
	}

	/**
	 * @param e
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#handleEvent(org.matsim.api.core.v01.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent e) {
		current.handleEvent(e);
	}

	/**
	 * @param event
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#handleEvent(org.matsim.api.core.v01.events.VehicleEntersTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		current.handleEvent(event);
	}

	/**
	 * @param event
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#handleEvent(org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		current.handleEvent(event);
	}

	/**
	 * @param event
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#handleEvent(org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent)
	 */
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		current.handleEvent(event);
	}

	/**
	 * @param event
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#handleEvent(org.matsim.api.core.v01.events.VehicleAbortsEvent)
	 */
	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		current.handleEvent(event);
	}

	/**
	 * @param iteration
	 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculator#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// Ringtausch:
		TravelTimeCalculator tmp = previous ;
		previous = current ;
		current = tmp ;

		current.reset(iteration) ;
	}

	@Override 
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return previous.getLinkTravelTime(link.getId(), time) ;
	}

}
