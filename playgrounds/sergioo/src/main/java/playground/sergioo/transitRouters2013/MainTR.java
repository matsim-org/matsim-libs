package playground.sergioo.transitRouters2013;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
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

import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWS;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculatorTuple;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeStuckCalculator;

public class MainTR {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]);
		int numTests = 100;
		//saveRoutes(numTests, startTime, endTime);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		WaitTimeStuckCalculator waitTimeCalculator2 = new WaitTimeStuckCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		StopStopTimeCalculatorTuple stopStopTimeCalculator2 = new StopStopTimeCalculatorTuple(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(waitTimeCalculator2);
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator2);
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetwork network = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkWW networkWW = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkTravelTimeAndDisutility travelFunction = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunctionWW = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), networkWW, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWS = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, scenario.getNetwork(), networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionW2S = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, scenario.getNetwork(), networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator2.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunctionWW2 = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), networkWW, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator2.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWS2 = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, scenario.getNetwork(), networkWW, waitTimeCalculator2.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionW2S2 = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, scenario.getNetwork(), networkWW, waitTimeCalculator2.getWaitTimes(), stopStopTimeCalculator2.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterImpl transitRouter = new TransitRouterImpl(transitRouterConfig, preparedTransitSchedule, network, travelFunction, travelFunction);
		TransitRouterVariableImpl transitRouterWW = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWW, networkWW, scenario.getNetwork());
		TransitRouterVariableImpl transitRouterWS = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWS, networkWW, scenario.getNetwork());
		TransitRouterVariableImpl transitRouterW2S = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionW2S, networkWW, scenario.getNetwork());
		TransitRouterVariableImpl transitRouterWW2 = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWW2, networkWW, scenario.getNetwork());
		TransitRouterVariableImpl transitRouterWS2 = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWS2, networkWW, scenario.getNetwork());
		TransitRouterVariableImpl transitRouterW2S2 = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionW2S2, networkWW, scenario.getNetwork());
		List<Leg> path = null;
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream("C:/Users/sergioo/workspace2/playgrounds/sergioo/data/routes.dat"));
		Coord[] origin = (Coord[]) ois.readObject(), destination = (Coord[]) ois.readObject();
		double[] dayTime = (double[]) ois.readObject();
		ois.close();
		long time;
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterW2S2.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" W2S2");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWS2.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WS2");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouter.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" W");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWS.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WS");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterW2S.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" W2S");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWW.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WW");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWW2.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WW2");
	}

	private static void saveRoutes(int numTests, double startTime, double endTime) throws FileNotFoundException, IOException {
		Coord[] origin = new Coord[numTests], destination = new Coord[numTests];
		double[] dayTime = new double[numTests];
		for(int i=0; i<numTests; i++) {
			origin[i] = new CoordImpl(346469+(389194-346469)*Math.random(), 137211+(162536-137211)*Math.random());
			destination[i] = new CoordImpl(346469+(389194-346469)*Math.random(), 137211+(162536-137211)*Math.random());
			dayTime[i] = Math.random()*(endTime-startTime)+startTime;
			//375009, 153261
		}
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("C:/Users/sergioo/workspace2/playgrounds/sergioo/data/routes.dat"));
		oos.writeObject(origin);
		oos.writeObject(destination);
		oos.writeObject(dayTime);
		oos.close();
	}

}
