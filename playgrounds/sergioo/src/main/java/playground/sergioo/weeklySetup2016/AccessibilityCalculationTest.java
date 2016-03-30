package playground.sergioo.weeklySetup2016;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.accessibility2013.MultiDestinationDijkstra;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.eventsBasedPTRouter.SerializableLinkTravelTimes;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterVariableImpl;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;

public class AccessibilityCalculationTest {

	public enum SimpleCategory {
		
		/*SHOP_LOW,*/
		SHOP_HIGH/*,
		BUSINESS,
		NEED, 
		EAT_HIGH,
		EAT_LOW,
		FUN,
		SPORT,
		CULTURAL,
		HEALTH,
		RELIGION*/;
		
	}

	private static final int NUM_CAR_FILES = 4;
	private static final double WALK_SPEED = 4.0*1000/3600;
	private static final double WALK_BL = 1.3;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);
		String line = reader.readLine();
		Set<String> fixedNodesCar = new HashSet<>();
		Set<String> fixedNodesPT = new HashSet<>();
		while(line!=null) {
			String[] parts = line.split(",");
			fixedNodesCar.add(parts[3]);
			fixedNodesPT.add(parts[4]);
			line = reader.readLine();
		}
		reader.close();
		System.out.println("Postal codes done! "+fixedNodesCar.size()+" "+fixedNodesPT.size());
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		Map<String, Set<Tuple<Coord, Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>>>> flexibleLocations = new HashMap<>();
		reader = IOUtils.getBufferedReader(args[1]);
		line = reader.readLine();
		Set<String> flexibleNodesCar = new HashSet<>();
		Set<String> flexibleNodesPT = new HashSet<>();
		while(line!=null) {
			String[] parts = line.split(",");
			Set<Tuple<Coord, Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>>> locs = flexibleLocations.get(parts[7]);
			if(locs == null) {
				locs = new HashSet<>();
				flexibleLocations.put(parts[7],locs);
			}
			locs.add(new Tuple<>(new Coord(Double.parseDouble(parts[1]),Double.parseDouble(parts[2])),new Tuple<>(new Tuple<>(new Tuple<>(parts[3], Double.parseDouble(parts[4])), new Tuple<>(parts[5], Double.parseDouble(parts[6]))), Integer.parseInt(parts[8]))));
			flexibleNodesCar.add(parts[3]);
			flexibleNodesPT.add(parts[5]);
			line = reader.readLine();
		}
		reader.close();
		System.out.println("Flexible locations done! "+flexibleNodesCar.size()+" "+flexibleNodesPT.size());
		date = new Date();
		System.out.println(dateFormat.format(date));
		Map<String, Map<String, Double>> carTimes = new HashMap<>();
		/*try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[3]));
			carTimes = (Map<String, Map<String, Double>>) ois.readObject();
			ois.close();
		}
		catch(Exception e) {
			for(int i=0; i<NUM_CAR_FILES; i++) {
				int n=0;
				reader = IOUtils.getBufferedReader(args[2]+i+".csv.gz");
				line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(",");
					if(fixedNodesCar.contains(parts[0]) && flexibleNodesCar.contains(parts[1]) && parts[2].equals(8*3600+"")) {
						if(++n%5000000==0)
							System.out.println(n);
						Map<String, Double> map = carTimes.get(parts[0]);
						if(map==null) {
							map = new HashMap<>();
							carTimes.put(parts[0], map);
						}
						map.put(parts[1], Double.parseDouble(parts[4]));
					}
					line = reader.readLine();
				}
				reader.close();
				System.out.println(i+" file");
				date = new Date();
				System.out.println(dateFormat.format(date));
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[3]));
			oos.writeObject(carTimes);
			oos.close();
		}
		System.out.println("Car times done! "+carTimes.size()+" "+carTimes.values().iterator().next().size());
		date = new Date();
		System.out.println(dateFormat.format(date));*/
		Map<String, Map<String, Double>> ptTimes = new HashMap<>();
		/*reader = IOUtils.getBufferedReader(args[4]);
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			if(fixedNodesPT.contains(parts[0]) && flexibleNodesPT.contains(parts[1]) && parts[2].equals(8*3600+"")) {
				Map<String, Double> map = ptTimes.get(parts[0]);
				if(map==null) {
					map = new HashMap<>();
					ptTimes.put(parts[0], map);
				}
				map.put(parts[1], Double.parseDouble(parts[4]));
			}
			line = reader.readLine();
		}
		reader.close();
		System.out.println("PT times done!");
		date = new Date();
		System.out.println(dateFormat.format(date));*/
		
		Config config = ConfigUtils.loadConfig(args[6]);
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[7]);
		new TransitScheduleReader(scenario).readFile(args[8]);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[9]));
		SerializableLinkTravelTimes times = (SerializableLinkTravelTimes) ois.readObject();
		WaitTimeCalculatorSerializable waitTimeCalculator = (WaitTimeCalculatorSerializable) ois.readObject();
		StopStopTimeCalculatorSerializable stopStopTimeCalculator = (StopStopTimeCalculatorSerializable) ois.readObject();
		ois.close();
		TravelDisutility disutilityFunction = (new OnlyTimeDependentTravelDisutilityFactory()).createTravelDisutility(times);
		TransitRouterEventsWSFactory factory = new TransitRouterEventsWSFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		TransitRouterVariableImpl transitRouter = (TransitRouterVariableImpl) factory.get();
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, times, preProcessDijkstra);
		
		Map<String,PrintWriter> writers = new HashMap<>();
		writers.put("738099", new PrintWriter("./data/weekly/accessibility/tt738099.csv"));
		writers.put("730305", new PrintWriter("./data/weekly/accessibility/tt730305.csv"));
		int n=0;
		boolean modCar = false;
		reader = IOUtils.getBufferedReader(args[0]);
		line = reader.readLine();
		BufferedWriter writer = IOUtils.getBufferedWriter(args[5]);
		while(line!=null) {
			if(++n%1000==0) {
				System.out.println(n);
				date = new Date();
				System.out.println(dateFormat.format(date));
			}
			String[] parts = line.split(",");
			Map<String, Double> cartTimes = carTimes.get(parts[3]);
			if(cartTimes == null) {
				modCar= true;
				Set<Node> nodes = new HashSet<>();
				for(String node:flexibleNodesCar)
					nodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId(node)));
				Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().get(Id.createNodeId(parts[3])), nodes, 8*3600, null, null);
				cartTimes = new HashMap<>();
				for(String node:flexibleNodesCar)			
					cartTimes.put(node, paths.get(Id.createNodeId(node)).travelTime);
				//carTimes.put(parts[3], cartTimes);
			}
			Map<String, Double> pttTimes = ptTimes.get(parts[5]);
			if(pttTimes == null) {
				Set<Id<Node>> nodeIds = new HashSet<>();
				for(String node:flexibleNodesPT)
					nodeIds.add(Id.createNodeId(node));
				Map<Id<Node>, Path> paths = transitRouter.calcPathRoutes(Id.createNodeId(parts[5]), nodeIds, 8*3600, null);
				pttTimes = new HashMap<>();
				for(String node:flexibleNodesPT) {
					Path path = paths.get(Id.createNodeId(node));
					pttTimes.put(node, path!=null?path.travelTime:CoordUtils.calcEuclideanDistance(transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(parts[5])).getCoord(), transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(node)).getCoord())*WALK_BL/WALK_SPEED);
				}
				//ptTimes.put(parts[5], pttTimes);
			}
			PrintWriter writerI = writers.get(parts[0]);
			for(SimpleCategory simpleCategory:SimpleCategory.values()) {
				Set<Tuple<Coord,Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>>> locs = flexibleLocations.get(simpleCategory.name());
				double carAcc = 0, ttSum = 0;
				double carAccB = 0, ttSumB = 0;
				for(Tuple<Coord,Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>> tuple:locs) {
					Double carTT = cartTimes.get(tuple.getSecond().getFirst().getFirst().getFirst());
					if(carTT==null) {
						modCar = true;
						Set<Node> nodes = new HashSet<>();
						nodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId(tuple.getSecond().getFirst().getFirst().getFirst())));
						Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().get(Id.createNodeId(parts[3])), nodes, 8*3600, null, null);
						carTT = paths.get(Id.createNodeId(tuple.getSecond().getFirst().getFirst().getFirst())).travelTime;
						cartTimes.put(tuple.getSecond().getFirst().getFirst().getFirst(), carTT);
					}
					double tt = carTT + tuple.getSecond().getFirst().getFirst().getSecond() + Double.parseDouble(parts[4]);
					writerI.println(tuple.getFirst().getX()+","+tuple.getFirst().getY()+",car,"+tt+","+tuple.getSecond().getSecond());
					carAcc += tuple.getSecond().getSecond()/tt;
					ttSum += 1/tt;
					carAccB += tuple.getSecond().getSecond()*tt;
					ttSumB += tuple.getSecond().getSecond();
				}
				carAcc/=ttSum;
				carAccB/=ttSumB;
				double ptAcc = 0; ttSum = 0;
				double ptAccB = 0; ttSumB = 0;
				for(Tuple<Coord,Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>> tuple:locs) {
					Double ptTT = pttTimes.get(tuple.getSecond().getFirst().getSecond().getFirst());
					if(ptTT==null) {
						Set<Id<Node>> nodeIds = new HashSet<>();
						nodeIds.add(Id.createNodeId(tuple.getSecond().getFirst().getSecond().getFirst()));
						Map<Id<Node>, Path> paths = transitRouter.calcPathRoutes(Id.createNodeId(parts[5]), nodeIds, 8*3600, null);
						Path path = paths.get(Id.createNodeId(tuple.getSecond().getFirst().getSecond().getFirst()));
						ptTT = path!=null?path.travelTime:CoordUtils.calcEuclideanDistance(transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(parts[5])).getCoord(), transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(tuple.getSecond().getFirst().getSecond().getFirst())).getCoord())*WALK_BL/WALK_SPEED;
						pttTimes.put(tuple.getSecond().getFirst().getSecond().getFirst(), ptTT);
					}
					double tt = ptTT + tuple.getSecond().getFirst().getSecond().getSecond() + Double.parseDouble(parts[6]);
					writerI.println(tuple.getFirst().getX()+","+tuple.getFirst().getY()+",pt,"+tt+","+tuple.getSecond().getSecond());
					ptAcc += tuple.getSecond().getSecond()/tt;
					ttSum += 1/tt;
					ptAccB += tuple.getSecond().getSecond()*tt;
					ttSumB += tuple.getSecond().getSecond();
				}
				ptAcc/=ttSum;
				ptAccB/=ttSumB;
				writer.write(parts[0]+","+parts[1]+","+parts[2]+","+simpleCategory.name()+","+carAcc+","+ptAcc+","+carAccB+","+ptAccB);
				writer.newLine();
			}
			writerI.close();
			line = reader.readLine();
		}
		reader.close();
		writer.close();
		System.out.println("Accessibilities done!");
		date = new Date();
		System.out.println(dateFormat.format(date));
		if(modCar) {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[3]));
			oos.writeObject(carTimes);
			oos.close();
		}
	}

}
