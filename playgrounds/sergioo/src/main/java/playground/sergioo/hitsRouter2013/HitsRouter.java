package playground.sergioo.hitsRouter2013;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeStuckCalculator;

import java.util.HashSet;
import java.util.Set;

public class HitsRouter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]);
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetworkWW network = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunction = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), network, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, new PreparedTransitSchedule(scenario.getTransitSchedule()));
		TransitRouterVariableImpl transitRouterVariableImpl = new TransitRouterVariableImpl(transitRouterConfig, travelFunction, network, scenario.getNetwork());
		for(int p=0;p<1000;p++) {
			Set<TransitLine> lines = new HashSet<TransitLine>();
			for(int p2=0;p2<1000;p2++)
				lines.add(scenario.getTransitSchedule().getTransitLines().get(""));
			transitRouterVariableImpl.setAllowedLines(lines);
			Path path = transitRouterVariableImpl.calcPathRoute(new CoordImpl("", ""), new CoordImpl("", ""), new Double(""), null);
		}
	}

}
