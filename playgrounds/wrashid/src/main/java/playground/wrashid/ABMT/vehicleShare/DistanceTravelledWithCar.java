package playground.wrashid.ABMT.vehicleShare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * 
 * @author wrashid
 *
 */
public class DistanceTravelledWithCar implements LinkEnterEventHandler, PersonArrivalEventHandler, 
PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	public static DoubleValueHashMap<Id> distanceTravelled;
	private Network network;
	
	Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

	public DistanceTravelledWithCar(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		distanceTravelled = new DoubleValueHashMap<Id>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double linkLength = network.getLinks().get(event.getLinkId()).getLength();
		distanceTravelled.incrementBy(delegate.getDriverOfVehicle(event.getVehicleId()), linkLength);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
			double linkLength = network.getLinks().get(event.getLinkId()).getLength();
			distanceTravelled.incrementBy(event.getPersonId(), linkLength / 2);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double linkLength = network.getLinks().get(event.getLinkId()).getLength();
		distanceTravelled.incrementBy(event.getPersonId(), linkLength / 2);
	}
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

}
