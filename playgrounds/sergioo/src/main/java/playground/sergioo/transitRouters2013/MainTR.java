package playground.sergioo.transitRouters2013;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWS;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeCalculator;

public class MainTR {

	public static int numCostsAsked;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]);
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetwork network = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkWW networkWW = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkTravelTimeAndDisutility travelFunction = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunctionWW = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), networkWW, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, new PreparedTransitSchedule(scenario.getTransitSchedule()));
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWS = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, scenario.getNetwork(), networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, new PreparedTransitSchedule(scenario.getTransitSchedule()));
		TransitRouterImpl transitRouter = new TransitRouterImpl(transitRouterConfig, preparedTransitSchedule, network, travelFunction, travelFunction);
		TransitRouterVariableImpl transitRouterWW = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWW, networkWW, scenario.getNetwork());
		TransitRouterVariableImpl transitRouterWS = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWS, networkWW, scenario.getNetwork());
		int numTests = 500;
		Coord[] origin = new Coord[numTests], destination = new Coord[numTests]; 
		for(int i=0; i<numTests; i++) {
			origin[i] = new CoordImpl(346469+(389194-346469)*Math.random(), 137211+(162536-137211)*Math.random());
			destination[i] = new CoordImpl(375009, 153261);
		}
		double dayTime = 8*3600;
		List<Leg> path;
		long time = System.currentTimeMillis();
		numCostsAsked = 0;
		for(int i=0; i<numTests; i++)
			path = transitRouter.calcRoute(origin[i], destination[i], dayTime, null);
		System.out.println(System.currentTimeMillis()-time+" "+numCostsAsked);
		time = System.currentTimeMillis();
		numCostsAsked = 0;
		for(int i=0; i<numTests; i++)
			path = transitRouterWW.calcRoute(origin[i], destination[i], dayTime, null);
		System.out.println(System.currentTimeMillis()-time+" "+numCostsAsked);
		time = System.currentTimeMillis();
		numCostsAsked = 0;
		for(int i=0; i<numTests; i++)
			path = transitRouterWS.calcRoute(origin[i], destination[i], dayTime, null);
		System.out.println(System.currentTimeMillis()-time+" "+numCostsAsked);
	}

}
