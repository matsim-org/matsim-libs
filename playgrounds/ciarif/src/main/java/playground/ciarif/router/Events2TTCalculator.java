package playground.ciarif.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class Events2TTCalculator {
	public static TravelTimeCalculator getTravelTimeCalculator(Scenario scenario, String eventsFile) {
		// reading the network
		//ScenarioImpl scenario = new ScenarioImpl();
		//NetworkLayer network = scenario.getNetwork();
		//new MatsimNetworkReader(scenario).readFile(networkFile);

		TravelTimeCalculator ttc = new TravelTimeCalculator(scenario.getNetwork(),3600,30*3600, scenario.getConfig().travelTimeCalculator());
		//SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().charyparNagelScoring()));
		//TTimeMatrixCalculator ttmc = new TTimeMatrixCalculator(parseL2ZMapping(mapfile),hours,st,network);

		// creating events object and assign handlers
		EventsManagerImpl events = new EventsManagerImpl();
		//events.addHandler(ttmc);
		events.addHandler(ttc);
		
		// reading events.  Will do all the processing as side effect.
		System.out.println("processing events file...");
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsFile);
		System.out.println("done.");
		
		return ttc;
	}
}
