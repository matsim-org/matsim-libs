package playground.wrashid.oswald;

import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.Wait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
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

	private static class CollectLocationOfArrivedCars implements AgentArrivalEventHandler, AgentDepartureEventHandler {

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
		public void handleEvent(AgentDepartureEvent event) {
			if (event.getTime() < timeOfSnapShotInSeconds) {
				getArrivals().remove(event.getPersonId());
			}
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			if (event.getTime() < timeOfSnapShotInSeconds) {
				getArrivals().put(event.getPersonId(), event.getLinkId());
			}
		}

		public HashMap<Id, Id> getArrivals() {
			return arrivals;
		}

	}

}
