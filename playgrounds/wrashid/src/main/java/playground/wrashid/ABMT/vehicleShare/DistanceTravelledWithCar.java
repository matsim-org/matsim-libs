package playground.wrashid.ABMT.vehicleShare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.controler.Controler;

/**
 * 
 * @author wrashid
 *
 */
public class DistanceTravelledWithCar implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {

	public static DoubleValueHashMap<Id> distanceTravelled;
	private Network network;

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
		distanceTravelled.incrementBy(event.getPersonId(), linkLength);
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

}
