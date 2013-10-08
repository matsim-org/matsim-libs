package playground.wrashid.oswald;

import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.objenesis.instantiator.basic.NewInstanceInstantiator;

import playground.wrashid.bodenbender.StudyArea;


/**
 * prints out coordinates of all parked vehicles at 'timeOfSnapShotInSeconds'
 * 
 * @author wrashid
 * 
 */
public class ParkingDemandInitialization {

	public static void main(String[] args) {
		String eventsFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";

		double timeOfSnapShotInSeconds = 7 * 3600;

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		CollectLocationOfArrivedCars printLocationOfArrivedCars = new CollectLocationOfArrivedCars(timeOfSnapShotInSeconds);

		events.addHandler(printLocationOfArrivedCars);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		HashMap<Id, Id> arrivals = printLocationOfArrivedCars.getArrivals();

		Network network = GeneralLib.readNetwork("H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz");

		System.out.println("agentId\tx\ty");
		
		for (Id agentId : arrivals.keySet()) {
			Coord linkCoord = network.getLinks().get(arrivals.get(agentId)).getCoord();
			
			if (StudyArea.isInStudyArea(linkCoord)){
				System.out.println(agentId + "\t" + linkCoord.getX() + "\t" + linkCoord.getY());
			}
			
		}

	}

	private static class CollectLocationOfArrivedCars implements PersonArrivalEventHandler, PersonDepartureEventHandler {

		private final double timeOfSnapShotInSeconds;
		// key: agentId, value:linkId
		private HashMap<Id, Id> arrivals = new HashMap<Id, Id>();

		public CollectLocationOfArrivedCars(double timeOfSnapShotInSeconds) {
			this.timeOfSnapShotInSeconds = timeOfSnapShotInSeconds;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getTime() < timeOfSnapShotInSeconds) {
				getArrivals().remove(event.getPersonId());
			}
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getTime() < timeOfSnapShotInSeconds) {
				getArrivals().put(event.getPersonId(), event.getLinkId());
			}
		}

		public HashMap<Id, Id> getArrivals() {
			return arrivals;
		}

	}

}
