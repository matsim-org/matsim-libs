package playground.sergioo.weeklySimulation.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class RouterTester {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, args[0]);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[1]);
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(travelTimeCalculator);
		(new MatsimEventsReader(eventsManager)).readFile(args[2]);
		TravelDisutility disutilityFunction = (new Builder( TransportMode.car, config.planCalcScore() )).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		Dijkstra dijkstra = new Dijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes());
		for(int i=0; i<6*24*3600; i+=1800) {
			Path path = dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().get(Id.createNodeId(1380035722)), scenario.getNetwork().getNodes().get(Id.createNodeId(1380024014)), i, null, null);
			if(path==null)
				path = dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().get(Id.createNodeId(1380035722)), scenario.getNetwork().getNodes().get(Id.createNodeId(1380024014)), i, null, null);
		}
		/*NetworkLegRouter router = new NetworkLegRouter(scenario.getNetwork(), new Dijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes()), new ModeRouteFactory());
		router.routeLeg(person, leg, fromAct, toAct, depTime);*/
	}
	
}
