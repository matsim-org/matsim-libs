package playground.sergioo.transitRouters2013;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import playground.sergioo.singapore2012.transitRouterVariable.*;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculatorTuple;
import playground.sergioo.singapore2012.transitRouterVariable.vehicleOccupancy.VehicleOccupancyCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeStuckCalculator;

import java.io.*;
import java.util.List;

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
		(new VehicleReaderV1(((ScenarioImpl)scenario).getTransitVehicles())).readFile(args[8]);
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		WaitTimeStuckCalculator waitTimeStuckCalculator = new WaitTimeStuckCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		StopStopTimeCalculatorTuple stopStopTimeCalculatorTuple = new StopStopTimeCalculatorTuple(scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		VehicleOccupancyCalculator vehicleOccupancyCalculator = new VehicleOccupancyCalculator(scenario.getTransitSchedule(), ((ScenarioImpl)scenario).getTransitVehicles(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(waitTimeStuckCalculator);
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculatorTuple);
		eventsManager.addHandler(travelTimeCalculator);
		eventsManager.addHandler(vehicleOccupancyCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetwork network = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), transitRouterConfig.getBeelineWalkConnectionDistance());
		TransitRouterNetworkWW networkWW = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.getBeelineWalkConnectionDistance());
		TransitRouterNetworkTravelTimeAndDisutility travelFunction = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunctionWW = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), networkWW, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunctionWstuckW = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), networkWW, travelTimeCalculator.getLinkTravelTimes(), waitTimeStuckCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWS = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWStuple = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculatorTuple.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWstuckS = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, networkWW, waitTimeStuckCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWS travelFunctionWstuckStuple = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig, networkWW, waitTimeStuckCalculator.getWaitTimes(), stopStopTimeCalculatorTuple.getStopStopTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWSV travelFunctionWSV = new TransitRouterNetworkTravelTimeAndDisutilityWSV(transitRouterConfig, networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWSV travelFunctionWStupleV = new TransitRouterNetworkTravelTimeAndDisutilityWSV(transitRouterConfig, networkWW, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculatorTuple.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWSV travelFunctionWstuckSV = new TransitRouterNetworkTravelTimeAndDisutilityWSV(transitRouterConfig, networkWW, waitTimeStuckCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterNetworkTravelTimeAndDisutilityWSV travelFunctionWstuckStupleV = new TransitRouterNetworkTravelTimeAndDisutilityWSV(transitRouterConfig, networkWW, waitTimeStuckCalculator.getWaitTimes(), stopStopTimeCalculatorTuple.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		TransitRouterImpl transitRouter = new TransitRouterImpl(transitRouterConfig, preparedTransitSchedule, network, travelFunction, travelFunction);
		TransitRouterVariableImpl transitRouterWW = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWW, networkWW);
		TransitRouterVariableImpl transitRouterWstuckW = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWstuckW, networkWW);
		TransitRouterVariableImpl transitRouterWS = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWS, networkWW);
		TransitRouterVariableImpl transitRouterWStuple = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWStuple, networkWW);
		TransitRouterVariableImpl transitRouterWstuckS = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWstuckS, networkWW);
		TransitRouterVariableImpl transitRouterWstuckStuple = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWstuckStuple, networkWW);
		TransitRouterVariableImpl transitRouterWSV = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWSV, networkWW);
		TransitRouterVariableImpl transitRouterWStupleV = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWStupleV, networkWW);
		TransitRouterVariableImpl transitRouterWstuckSV = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWstuckSV, networkWW);
		TransitRouterVariableImpl transitRouterWstuckStupleV = new TransitRouterVariableImpl(transitRouterConfig, travelFunctionWstuckStupleV, networkWW);
		List<Leg> path = null;
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream("C:/Users/sergioo/workspace2/playgrounds/sergioo/data/routes.dat"));
		Coord[] origin = (Coord[]) ois.readObject(), destination = (Coord[]) ois.readObject();
		double[] dayTime = (double[]) ois.readObject();
		ois.close();
		long time;
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouter.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" W");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWW.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WW");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWstuckW.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WstuckW");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWS.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WS");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWStuple.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WStuple");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWstuckS.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WstuckS");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWstuckStuple.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WstuckStuple");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWSV.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WSV");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWStupleV.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WStupleV");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWstuckSV.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WstuckSV");
		time = System.currentTimeMillis();
		for(int i=0; i<numTests; i++)
			path = transitRouterWstuckStupleV.calcRoute(origin[i], destination[i], dayTime[i], null);
		System.out.println(System.currentTimeMillis()-time+" "+path.size()+" WstuckStupleV");
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
