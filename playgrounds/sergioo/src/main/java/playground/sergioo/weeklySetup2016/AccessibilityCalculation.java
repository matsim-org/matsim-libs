package playground.sergioo.weeklySetup2016;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.probability.ContinuousRealDistribution;
import playground.sergioo.accessibility2013.MultiDestinationDijkstra;
import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.Purpose;
import playground.sergioo.hits2012Scheduling.IncomeEstimation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.eventsBasedPTRouter.SerializableLinkTravelTimes;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterVariableImpl;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;

public class AccessibilityCalculation extends Thread {
	
	private static Collection<Object> lines;
	
	private int t; 
	private Collection<String> fixeds;
	
	
	public AccessibilityCalculation(int t, Collection<String> fixeds) {
		super();
		this.t = t;
		this.fixeds = fixeds;
	}

	@Override
	public void run() {
		TravelDisutility disutilityFunction = (new OnlyTimeDependentTravelDisutilityFactory()).createTravelDisutility(times);
		TransitRouterEventsWSFactory factory = new TransitRouterEventsWSFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		TransitRouterVariableImpl transitRouter = (TransitRouterVariableImpl) factory.get();
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, times, preProcessDijkstra);
		boolean modCar = false;
		int n=0;
		for(String line:fixeds) {
			if(++n%1000==0) {
				System.out.println(n);
				date = new Date();
				System.out.println(t+","+dateFormat.format(date));
			}
			String[] parts = line.split(",");
			
			modCar= true;
			Set<Node> nodes = new HashSet<>();
			for(String node:flexibleNodesCar)
				nodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId(node)));
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().get(Id.createNodeId(parts[3])), nodes, 8*3600, null, null);
			HashMap<String, Double> cartTimes = new HashMap<>();
			for(String node:flexibleNodesCar)			
				cartTimes.put(node, paths.get(Id.createNodeId(node)).travelTime);
			Set<Id<Node>> nodeIds = new HashSet<>();
			for(String node:flexibleNodesPT)
				nodeIds.add(Id.createNodeId(node));
			paths = transitRouter.calcPathRoutes(Id.createNodeId(parts[5]), nodeIds, 8*3600, null);
			HashMap<String, Double> pttTimes = new HashMap<>();
			for(String node:flexibleNodesPT) {
				Path path = paths.get(Id.createNodeId(node));
				pttTimes.put(node, path!=null?path.travelTime:CoordUtils.calcEuclideanDistance(transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(parts[5])).getCoord(), transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(node)).getCoord())*WALK_BL/WALK_SPEED);
			}
			for(SimpleCategory simpleCategory:SimpleCategory.values()) {
				Set<Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>> locs = flexibleLocations.get(simpleCategory.name());
				ContinuousRealDistribution disCar = distributions.get(simpleCategory).get("car");
				ContinuousRealDistribution disPT = distributions.get(simpleCategory).get("car");
				double carAcc = 0, ttSum = 0;
				double carAccB = 0, ttSumB = 0;
				double carAccD = 0;
				double carAccE = 0;
				double carAccF = 0;
				double carAccG = 0;
				SortedSet<Double> ttsC = new TreeSet<>();
				SortedSet<Double> ttsE = new TreeSet<>();
				for(Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer> tuple:locs) {
					Double carTT = cartTimes.get(tuple.getFirst().getFirst().getFirst());
					if(carTT==null) {
						modCar = true;
						nodes = new HashSet<>();
						nodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId(tuple.getFirst().getFirst().getFirst())));
						paths = dijkstra.calcLeastCostPath(scenario.getNetwork().getNodes().get(Id.createNodeId(parts[3])), nodes, 8*3600, null, null);
						carTT = paths.get(Id.createNodeId(tuple.getFirst().getFirst().getFirst())).travelTime;
						cartTimes.put(tuple.getFirst().getFirst().getFirst(), carTT);
					}
					double tt = Math.max(MIN_TIME, carTT + tuple.getFirst().getFirst().getSecond() + Double.parseDouble(parts[4]));
					carAcc += tuple.getSecond()/tt;
					ttSum += 1/tt;
					carAccB += tuple.getSecond()*tt;
					ttSumB += tuple.getSecond();
					ttsC.add(tt/tuple.getSecond());
					double acc = disCar.probability(tt)*tuple.getSecond();
					carAccD += acc;
					ttsE.add(1/acc);
				}
				carAcc/=ttSum;
				carAccB/=ttSumB;
				double carAccC = 0;
				int l=0;
				for(Double tt:ttsC)
					if(l++<NUM_PLACES)
						carAccC+=1/tt;
					else
						break;
				l=0;
				for(Double tt:ttsE)
					if(l++<NUM_PLACES)
						carAccE+=1/tt;
					else
						break;
				l=0;
				for(Double tt:ttsE)
					if(l++<NUM_PLACES/5)
						carAccF+=1/tt;
					else
						break;
				l=0;
				for(Double tt:ttsE)
					if(l++<NUM_PLACES/10)
						carAccG+=1/tt;
					else
						break;
				double ptAcc = 0; ttSum = 0;
				double ptAccB = 0; ttSumB = 0;
				double ptAccD = 0;
				double ptAccE = 0;
				double ptAccF = 0;
				double ptAccG = 0;
				ttsC = new TreeSet<>();
				ttsE = new TreeSet<>();
				for(Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer> tuple:locs) {
					Double ptTT = pttTimes.get(tuple.getFirst().getSecond().getFirst());
					if(ptTT==null) {
						nodeIds = new HashSet<>();
						nodeIds.add(Id.createNodeId(tuple.getFirst().getSecond().getFirst()));
						paths = transitRouter.calcPathRoutes(Id.createNodeId(parts[5]), nodeIds, 8*3600, null);
						Path path = paths.get(Id.createNodeId(tuple.getFirst().getSecond().getFirst()));
						ptTT = path!=null?path.travelTime:CoordUtils.calcEuclideanDistance(transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(parts[5])).getCoord(), transitRouter.getTransitRouterNetwork().getNodes().get(Id.createNodeId(tuple.getFirst().getSecond().getFirst())).getCoord())*WALK_BL/WALK_SPEED;
						pttTimes.put(tuple.getFirst().getSecond().getFirst(), ptTT);
					}
					double tt = Math.max(MIN_TIME, ptTT + tuple.getFirst().getSecond().getSecond() + Double.parseDouble(parts[6]));
					ptAcc += tuple.getSecond()/tt;
					ttSum += 1/tt;
					ptAccB += tuple.getSecond()*tt;
					ttSumB += tuple.getSecond();
					ttsC.add(tt/tuple.getSecond());
					double acc = tuple.getSecond()*disPT.probability(tt);
					ptAccD += acc;
					ttsE.add(1/acc);
				}
				ptAcc/=ttSum;
				ptAccB/=ttSumB;
				double ptAccC = 0;
				l=0;
				for(Double tt:ttsC)
					if(l++<NUM_PLACES)
						ptAccC+=1/tt;
					else
						break;
				l=0;
				for(Double tt:ttsE)
					if(l++<NUM_PLACES)
						ptAccE+=1/tt;
					else
						break;
				l=0;
				for(Double tt:ttsE)
					if(l++<NUM_PLACES/5)
						ptAccF+=1/tt;
					else
						break;
				l=0;
				for(Double tt:ttsE)
					if(l++<NUM_PLACES/10)
						ptAccG+=1/tt;
					else
						break;
				lines.add(parts[0]+","+parts[1]+","+parts[2]+","+simpleCategory.name()+","+carAcc+","+ptAcc+","+carAccB+","+ptAccB+","+carAccC*3600/NUM_PLACES+","+ptAccC*3600/NUM_PLACES+","+carAccD+","+ptAccD+","+carAccE+","+ptAccE+","+carAccF+","+ptAccF+","+carAccG+","+ptAccG);
			}
		}
		System.out.println("Accessibilities "+t+" done!");
		date = new Date();
		System.out.println(dateFormat.format(date));
	}

	public enum SimpleCategory {
		
		SHOP_LOW,
		SHOP_HIGH,
		BUSINESS,
		NEED, 
		EAT_HIGH,
		EAT_LOW,
		FUN,
		SPORT,
		CULTURAL,
		HEALTH,
		RELIGION;
		
	}
	private static class Location {
		private String postalCode;
		private Coord coord;
		private Map<SimpleCategory, Integer> simpleCategories = new HashMap<>();
		
		private Location(String postalCode, Coord coord) {
			super();
			this.postalCode = postalCode;
			this.coord = coord;
		}
		private void addSimpleCategory(SimpleCategory simpleCategory, Integer num) {
			simpleCategories.put(simpleCategory, num);
		}
		
	}
	
	private static Map<String, Location> locations = new HashMap<>();
	private static Map<String, Location> fixed = new HashMap<>();
	private static Map<String, Household> households;
	//private static final int NUM_CAR_FILES = 4;
	private static final double WALK_SPEED = 4.0*1000/3600;
	private static final double WALK_BL = 1.3;
	private static final int NUM_PLACES = 50;
	private static final double MIN_TIME = 5.0*60;
	private static Set<String> FIXED_ACTIVITIES = new HashSet<String>(Arrays.asList(new String[]{Trip.Purpose.ACCOMP.text, Trip.Purpose.HOME.text, Trip.Purpose.WORK.text, Trip.Purpose.EDU.text,
			Trip.Purpose.WORK_FLEX.text, Trip.Purpose.P_U_D_O.text, Trip.Purpose.DRIVE.text}));
	private static Set<String> FLEX_ATIVITIES = new HashSet<String>(Arrays.asList(new String[]{Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text, Trip.Purpose.RELIGION.text, Trip.Purpose.MEDICAL.text}));

	private static SimpleDateFormat dateFormat;
	private static Date date;
	private static Set<String>  flexibleNodesCar;
	private static Set<String>  flexibleNodesPT;
	private static Scenario scenario;
	private static SerializableLinkTravelTimes times;
	private static WaitTimeCalculatorSerializable waitTimeCalculator;
	private static StopStopTimeCalculatorSerializable stopStopTimeCalculator;
	private static Map<SimpleCategory, Map<String, ContinuousRealDistribution>> distributions;
	private static Map<String, Set<Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>>> flexibleLocations;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, NumberFormatException, ParseException, InterruptedException {
		distributions = getDistributions(args);
		/*BufferedReader readerA = new BufferedReader(new FileReader("./data/weekly/R/xs.csv"));
		BufferedWriter writerA = IOUtils.getBufferedWriter("./data/weekly/R/ys4.csv");
		readerA.readLine();
		String lineA = readerA.readLine();
		ContinuousRealDistribution aa = distributions.get(SimpleCategory.SHOP_HIGH).get("pt");
		while(lineA!=null) {
			String[] parts = lineA.split(",");
			writerA.write(""+aa.probability(Double.parseDouble(parts[1])));
			writerA.newLine();
			lineA = readerA.readLine();
		}
		readerA.close();
		writerA.close();*/
		System.out.println("Distributions done");
		dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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
		date = new Date();
		System.out.println(dateFormat.format(date));
		flexibleLocations = new HashMap<>();
		reader = IOUtils.getBufferedReader(args[1]);
		line = reader.readLine();
		flexibleNodesCar = new HashSet<>();
		flexibleNodesPT = new HashSet<>();
		while(line!=null) {
			String[] parts = line.split(",");
			Set<Tuple<Tuple<Tuple<String, Double>, Tuple<String, Double>>, Integer>> locs = flexibleLocations.get(parts[7]);
			if(locs == null) {
				locs = new HashSet<>();
				flexibleLocations.put(parts[7],locs);
			}
			locs.add(new Tuple<>(new Tuple<>(new Tuple<>(parts[3], Double.parseDouble(parts[4])), new Tuple<>(parts[5], Double.parseDouble(parts[6]))), Integer.parseInt(parts[8])));
			flexibleNodesCar.add(parts[3]);
			flexibleNodesPT.add(parts[5]);
			line = reader.readLine();
		}
		reader.close();
		System.out.println("Flexible locations done! "+flexibleNodesCar.size()+" "+flexibleNodesPT.size());
		date = new Date();
		System.out.println(dateFormat.format(date));
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
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[7]);
		new TransitScheduleReader(scenario).readFile(args[8]);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[9]));
		times = (SerializableLinkTravelTimes) ois.readObject();
		waitTimeCalculator = (WaitTimeCalculatorSerializable) ois.readObject();
		stopStopTimeCalculator = (StopStopTimeCalculatorSerializable) ois.readObject();
		ois.close();
		lines = Collections.synchronizedCollection(new ArrayList<>());
		int numThreads = Integer.parseInt(args[11]);
		if(numThreads<1)
			return;
		AccessibilityCalculation[] threads = new AccessibilityCalculation[numThreads];
		Collection<String>[] allLines = new Collection[numThreads];
		for(int i=0; i<numThreads; i++)
			allLines[i] = new ArrayList<>();
		reader = IOUtils.getBufferedReader(args[0]);
		line = reader.readLine();
		int n=0;
		while(line!=null) {
			allLines[n++].add(line);
			if(n==numThreads)
				n=0;
			line = reader.readLine();
		}
		reader.close();
		for(int i=0; i<numThreads; i++) {
			threads[i] = new AccessibilityCalculation(i,allLines[i]);
			threads[i].start();
		}
		for(int i=0; i<numThreads; i++)
			threads[i].join();
		BufferedWriter writer = IOUtils.getBufferedWriter(args[5]);
		for(Object lineT:lines) {
			writer.write((String)lineT);
			writer.newLine();
		}
		writer.close();
		System.out.println("Accessibilities done!");
		date = new Date();
		System.out.println(dateFormat.format(date));
		/*if(modCar) {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[3]));
			oos.writeObject(carTimes);
			oos.writeObject(ptTimes);
			oos.close();
		}*/
	}

	private static Map<SimpleCategory, Map<String, ContinuousRealDistribution>> getDistributions(String[] args) throws NumberFormatException, IOException, ParseException, ClassNotFoundException {
		households = HitsReader.readHits(args[10]);
		IncomeEstimation.init();
		IncomeEstimation.setIncome(households);
		BufferedReader reader = new BufferedReader(new FileReader(args[1]));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			String id = parts[0];
			Location location = locations.get(id);
			if(location==null) {
				location = new Location(id, new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
				locations.put(id, location);
			}
			location.addSimpleCategory(SimpleCategory.valueOf(parts[7]), Integer.parseInt(parts[8]));
			line = reader.readLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader(args[0]));
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			String id = parts[0];
			Location location = fixed.get(id);
			if(location==null) {
				location = new Location(id, new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
				fixed.put(id, location);
			}
			line = reader.readLine();
		}
		reader.close();
		Config config = ConfigUtils.loadConfig(args[6]);
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[7]);
		Set<String> carMode = new HashSet<String>();
		carMode.add("car");
		NetworkImpl network = (NetworkImpl) NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(network, carMode);
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
		Dijkstra dijkstra = new Dijkstra(network, disutilityFunction, times, preProcessDijkstra);
		Map<SimpleCategory, Map<String, ContinuousRealDistribution>> distributions = new HashMap<>();
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				String prevAct = "";
				if(person.isStartHome())
					prevAct=Purpose.HOME.text;
				for(Trip trip:person.getTrips().values()) {
					if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
						Location location = locations.get(trip.getEndPostalCode());
						Location origin = fixed.get(trip.getStartPostalCode());
						if(location!=null && origin!=null && (prevAct.equals(Purpose.HOME.text)||prevAct.equals(Purpose.WORK.text)||prevAct.equals(Purpose.EDU.text))) {
							Node fromNode = ((NetworkImpl)network).getNearestNode(origin.coord);
							Node toNode = ((NetworkImpl)network).getNearestNode(location.coord);
							Path pathCar = dijkstra.calcLeastCostPath(fromNode, toNode, 8*3600, null, null);
							Path pathPt = transitRouter.calcPathRoute(origin.coord, location.coord, 8*3600, null);
							double walkCar = config.plansCalcRoute().getBeelineDistanceFactors().get("walk")*CoordUtils.calcEuclideanDistance(origin.coord, fromNode.getCoord())/config.plansCalcRoute().getTeleportedModeSpeeds().get("walk");
							walkCar += config.plansCalcRoute().getBeelineDistanceFactors().get("walk")*CoordUtils.calcEuclideanDistance(location.coord, toNode.getCoord())/config.plansCalcRoute().getTeleportedModeSpeeds().get("walk");
							for(SimpleCategory entry:location.simpleCategories.keySet()) {
								Map<String, ContinuousRealDistribution> distrs = distributions.get(entry);
								if(distrs==null) {
									distrs = new HashMap<>();
									distributions.put(entry, distrs);
								}
								ContinuousRealDistribution disCar = distrs.get("car");
								if(disCar==null) {
									disCar = new ContinuousRealDistribution(20);
									distrs.put("car", disCar);
								}
								disCar.addValue(pathCar.travelTime+walkCar);
								ContinuousRealDistribution disPT = distrs.get("pt");
								if(disPT==null) {
									disPT = new ContinuousRealDistribution(20);
									distrs.put("pt", disPT);
								}
								disPT.addValue(pathPt.travelTime);
							}
						}
					}
					prevAct = trip.getPurpose();
				}
			}
		return distributions;
	}

}
