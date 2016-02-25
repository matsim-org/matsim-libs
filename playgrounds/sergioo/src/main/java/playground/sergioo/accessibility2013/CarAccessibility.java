package playground.sergioo.accessibility2013;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.HashSet;
import java.util.Set;

public class CarAccessibility {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[2]);
		TravelDisutility disutilityFunction = (new Builder( TransportMode.car, scenario.getConfig().planCalcScore() )).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes(), preProcessDijkstra);
		Set<Node> nodes = new HashSet<Node>();
		for(Node node:scenario.getNetwork().getNodes().values())
			nodes.add(node);
		long time = System.currentTimeMillis();
		dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().values().iterator().next(), nodes, 8*3600, null, null);
		System.out.println(System.currentTimeMillis()-time);
	}

}
