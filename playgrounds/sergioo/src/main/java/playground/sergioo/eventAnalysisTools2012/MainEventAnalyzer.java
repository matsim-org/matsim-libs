package playground.sergioo.eventAnalysisTools2012;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;

public class MainEventAnalyzer {

	//Constants
	
	//Main
	/**
	 * @param args
	 * @throws IOException 
	 */
	/* Review of the given nodes 
	private final static String[] NODES = {"fl1380023697", "fl1380023696", "1380018339", "1380018369", "1380005056", "fl1380005050", "1380005051", "fl1380005104", "fl1380005105", "fl1380017582", "1380007412", "fl1380007413", "fl1380007414", "fl1380007415", "1380007417", "1380004301", "fl1380026056", "1380003771", "fl1380028836", "fl1380028837", "1380037616", "fl1380042204", "fl1380034444", "1380026126", "1380008911", "fl1380009667", "fl1380009665", "fl1380009666", "fl1380009668", "fl1380009669", "1380032058", "fl1380015859", "fl1380015843", "fl1380015844", "fl1380006426", "fl1380006427", "fl1380040290", "1380018558", "fl1380018556", "1380018557", "1380020283", "1380003526", "1380003527", "1380003528", "1380009663", "1380009664", "1380033576", "1380026356", "1380017055", "1380017054", "1380012775", "1380012773", "1380012774", "1380017592", "1380017593", "1380000030", "1380000031", "1380000032", "1380017358", "1380017359", "1380000027", "cl1380000028", "1380000029", "cl70234_1380029418_0", "1380029418", "1380031999", "1380009482", "1380009483", "1380009484", "1380029570", "1380015934", "1380030786", "1380030787", "1380028314", "cl16569_1380027040_0", "1380027040", "1380033272", "1380027345", "1380000233", "cl1380000231", "1380000232", "1380004109", "1380019523", "1380018689", "1380018690", "1380032270", "1380019741", "1380019739", "1380019740", "1380022838", "1380005392", "1380032271", "1380007256", "1380019743", "1380026078", "cl1380019534", "cl57073_1380019532_0", "cl1380019532", "1380019533", "cl3016_1380035914_0", "1380035914", "1380038486", "cl29981_1380035485_0", "1380035485", "1380031465", "1380026568", "cl30724_1380009568_0", "cl1380009568", "cl1380009569", "1380005328", "cl1380005329", "1380005330", "1380011984", "1380011975", "1380011976", "1380011981", "1380011982", "1380011991", "1380000760", "56658_1380013293_0", "1380013293", "1380013289", "1380003312", "1380038427", "1380032904", "1380017577", "1380036297", "1380028071", "1380029055", "1380017558", "1380017559", "1380020535", "1380020536", "1380026467", "1380026057", "1380018491", "1380018492", "1380009744", "1380009740", "1380002083", "1380025539", "1380033610", "1380019378", "56042_1380019176_0", "1380019176", "1380042135", "1380025865", "1380037697", "1380024474", "1380024475", "1380017969", "1380001256", "1380029038", "1380032546", "1380024999", "1380038103", "1380005364", "1380024910", "1380024911", "1380028605", "1380028598", "1380019026", "1380028398", "1380025211", "1380025210"};
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore6.xml");
		double dist = 0;
		for(int i=0; i<NODES.length-1; i++)
			for(Link link:scenario.getNetwork().getLinks().values())
				if(scenario.getNetwork().getNodes().get(Id.createNodeId(NODES[i])).equals(link.getFromNode()) && scenario.getNetwork().getNodes().get(Id.createNodeId(NODES[i+1])).equals(link.getToNode()))
					dist+=link.getLength();
		System.out.println(dist);
	}*/
	/* Increase capacity of bus links (25% people but 100% buses)
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore5.xml");
		for(Link link:scenario.getNetwork().getLinks().values())
			if(link.getAllowedModes().contains("bus") && link.getAllowedModes().contains("car"))
				link.setCapacity(link.getCapacity()+(link.getCapacity()/link.getNumberOfLanes())*3);
		new NetworkWriter(scenario.getNetwork()).write("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
	}*/
	/* OD matrix travel times
	public static void main(String[] args) throws IOException {
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		new NetworkCleaner().run(scenario.getNetwork());
		TravelDisutility travelMinCost =  new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(scenario.getNetwork());
		TravelTime timeFunction = new TravelTime() {	
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength()/link.getFreespeed();
			}
		};
		LeastCostPathCalculator leastCostPathCalculator = new FastDijkstra(scenario.getNetwork(), travelMinCost, timeFunction, preProcessData);
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/MTZ_Shortest_Path_Yanding.txt")));
		PrintWriter writer = new PrintWriter(new File("./data/MTZ_Shortest_Path_Yanding_Full.txt"));
		String line=reader.readLine();
		int i=0;
		while(line!=null) {
			String[] parts=line.split(";");
			Coord start = coordinateTransformation.transform(new CoordImpl(parts[3], parts[4]));
			Coord end = coordinateTransformation.transform(new CoordImpl(parts[5], parts[6]));
			Path path = leastCostPathCalculator.calcLeastCostPath(((NetworkImpl)scenario.getNetwork()).getNearestNode(start), ((NetworkImpl)scenario.getNetwork()).getNearestNode(end), 0, null, null);
			double distance = 0;
			for(Link link:path.links)
				distance+=link.getLength();
			writer.println(parts[0]+";"+path.travelCost+";"+distance);
			line=reader.readLine();
			i++;
			if(i%1000==0)
				System.out.println(i);
		}
		reader.close();
		writer.close();
	}*/
	/* Write a half of the plans
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		new MatsimPopulationReader(scenario).parse("./data/MATSim-Sin-2.0/input/population/routedPlans25.xml.gz");
		Set<Id<Person>> keys = new HashSet<Id<Person>>();
		int i=0;
		for(Id<Person> key:scenario.getPopulation().getPersons().keySet()) {
			keys.add(key);
			if(i>scenario.getPopulation().getPersons().size()/2)
				break;
			i++;
		}
		for(Id<Person> key:keys)
			scenario.getPopulation().getPersons().remove(key);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("./data/MATSim-Sin-2.0/input/population/routedPlans12.5.xml.gz");
	}*/
	/* Count vehicles
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((ScenarioImpl)scenario).getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("./data/MATSim-Sin-2.0/input/transit/transitScheduleWAM.xml");
		int numBuses = 0;
		int numTrains = 0;
		int time = 9*3600;
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(Departure departure:route.getDepartures().values())
					if(departure.getDepartureTime()<time && time<departure.getDepartureTime()+route.getStops().get(route.getStops().size()-1).getArrivalOffset())
						if(route.getTransportMode().contains("bus"))
							numBuses++;
						else
							numTrains++;
		System.out.println(numBuses+" "+numTrains);
	}*/
	/* Paint links where people is waiting
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		Set<Link> links = new HashSet<Link>();
		Map<Id<Link>, Double> linkWeights = new HashMap<Id<Link>, Double>();
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/chains_50.txt")));
		String line = reader.readLine();
		int i=0;
		while(line!=null) {
			line = reader.readLine();
			if(line!=null) {
				int lastOpen = line.lastIndexOf("(");
				String isCar = line.substring(lastOpen-3, lastOpen);
				if(!isCar.equals("car")) {
					String id = line.substring(lastOpen+1, line.lastIndexOf(")"));
					Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(id));
					if(link!=null) {
						i++;
						Double value = linkWeights.get(link.getId());
						if(value==null)
							value = 0.0;
						linkWeights.put(link.getId(), value+1);
						links.add(link);
					}
				}
				reader.readLine();
			}
		}
		System.out.println(i);
		VariableSizeSelectionNetworkPainter networkPainter = new VariableSizeSelectionNetworkPainter(scenario.getNetwork());
		networkPainter.setlinkWeights(linkWeights);
		JFrame window = new SimpleNetworkWindow("Links where people is waiting", networkPainter);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
	}*/
	/* Trip distance time bins
	public static void main(String[] args) throws IOException {
		final String CSV_SEPARATOR = "\t";
		final double WALK_FACTOR = 1.2;
		final double WALK_SPEED = 4*1000/3600;
		final double PT_SPEED = 17*1000/3600;
		final double CAR_SPEED = 2.05*PT_SPEED;
		final double lengthBinSize = 1000;
		final double maxLength = 48000;
		String[] modes = {"car", "pt"};
		SortedMap<Double,Map<String, Integer>> numberOfTravelers = new TreeMap<Double,Map<String, Integer>>();
		for(double l=lengthBinSize; l<=maxLength+lengthBinSize-1; l+=lengthBinSize) {
			Map<String, Integer> binMap = new HashMap<String, Integer>();
			for(String mode:modes)
				binMap.put(mode, 0);
			numberOfTravelers.put(l, binMap);
		}
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		new MatsimPopulationReader(scenario).parse("./data/MATSim-Sin-2.0/input/population/plansShort.xml.gz");
		((ScenarioImpl)scenario).getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("./data/MATSim-Sin-2.0/input/transit/transitScheduleWAM.xml");
		for(Person person:scenario.getPopulation().getPersons().values())
			for(int p=0; p<person.getSelectedPlan().getPlanElements().size(); p++) {
				PlanElement planElement = person.getSelectedPlan().getPlanElements().get(p);
				if(planElement instanceof Leg) {
					Leg leg = (Leg)planElement;
					double distance = 0;
					if(leg.getMode().equals("car"))
						if(leg.getRoute() instanceof NetworkRoute)
							for(Id<Link> linkId:((NetworkRoute)leg.getRoute()).getLinkIds())
								distance += scenario.getNetwork().getLinks().get(linkId).getLength();
						else
							distance += leg.getTravelTime()*CAR_SPEED;
					else if(leg.getMode().equals("transit_walk"))
						while(!(planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))) {
							if(planElement instanceof Leg) {
								leg = (Leg)planElement;
								if(leg.getMode().equals("transit_walk"))
									if(leg.getRoute()!=null)
										distance += WALK_FACTOR*CoordUtils.calcDistance(scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()).getFromNode().getCoord(), scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getToNode().getCoord());
									else
										distance += leg.getTravelTime()*WALK_SPEED;
								else if(leg.getMode().equals("pt")) {
									if(leg.getRoute()!=null) {
										String[] description = ((GenericRoute)leg.getRoute()).getRouteDescription().split("===");
										boolean inRoute=false;
										ROUTE:
										for(Id<Link> linkId:scenario.getTransitSchedule().getTransitLines().get(Id.create(description[2], TransitLine.class)).getRoutes().get(Id.create(description[3], TransitRoute.class)).getRoute().getLinkIds()) {
											if(linkId.toString().equals(description[1]))
												inRoute=true;
											if(inRoute)
												distance += scenario.getNetwork().getLinks().get(linkId).getLength();
											if(linkId.toString().equals(description[4]))
												break ROUTE;
										}
									}
									else
										distance += leg.getTravelTime()*PT_SPEED ;
								}
								else
									throw new RuntimeException("Unknown pt leg");
							}
							p++;
							planElement = person.getSelectedPlan().getPlanElements().get(p);
						}
					else
						throw new RuntimeException("Unknown leg: "+leg.getMode());
					INTERVAL:
					for(Double interval:numberOfTravelers.keySet()) {
						String mode = leg.getMode().equals("car")?"car":"pt";
						if(distance<interval) {
							numberOfTravelers.get(interval).put(mode,numberOfTravelers.get(interval).get(mode)+1);
							break INTERVAL;
						}
					}
				}
			}
		PrintWriter writer = new PrintWriter(new File("./data/tripDistanceMode.txt"));
		writer.print("interval");
		for(String mode:modes)
			writer.print(CSV_SEPARATOR+mode);
		writer.println();
		for(Double interval:numberOfTravelers.keySet()) {
			writer.print(interval);
			for(String mode:modes)
				writer.print(CSV_SEPARATOR+numberOfTravelers.get(interval).get(mode));
			writer.println();
		}
		writer.close();
	}*/
	/* Counts?
	public static void main(String[] args) throws IOException {
		Counts counts = new Counts();
		new MatsimCountsReader(counts).parse("./data/MATSim-Sin-2.0/input/counts/countsCar.xml");
		CountTimeBins countTimeBins = new CountTimeBins(new String[]{"car", "pt"}, counts.getCounts().keySet(), 108000);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(countTimeBins);
		new MatsimEventsReader(events).readFile("./data/MATSim-Sin-2.0/output/ITERS/it.10/10.events.xml.gz");
	}
	/*Congested travel times and distances
	public static void main(String[] args) throws IOException {
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore6.xml");
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReaderV1(scenario).parse("./data/MATSim-Sin-2.0/input/transit/transitScheduleWAM.xml");
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario2).parse("./data/MATSim-Sin-2.0/input/network/singapore6.xml");
		Set<Id<Link>> removedLinkIds = new HashSet<Id<Link>>();
		for(Link link:scenario2.getNetwork().getLinks().values())
			if(!link.getAllowedModes().contains("car"))
				removedLinkIds.add(link.getId());
		for(Id<Link> linkId:removedLinkIds)
			scenario2.getNetwork().removeLink(linkId);
		new NetworkCleaner().run(scenario2.getNetwork());
		TravelTimeCalculator ttc = new TravelTimeCalculator(scenario.getNetwork(), 15*60, 30*3600, scenario.getConfig().travelTimeCalculator());
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(ttc);
		new MatsimEventsReader(events).readFile("./data/MS2/output/ITERS/it.100/100.events.xml.gz");
		TravelDisutility tcc = new TravelCostCalculatorFactoryImpl().createTravelDisutility(ttc, scenario.getConfig().planCalcScore());
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(scenario2.getNetwork());
		IntermodalLeastCostPathCalculator leastCostPathCalculator = new FastDijkstra(scenario2.getNetwork(), tcc, ttc, preProcessData);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
		TransitRouter transitRouter = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig).createTransitRouter();
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/home_work_locations.csv")));
		PrintWriter writer = new PrintWriter(new File("./data/home_work_tt.csv"));
		writer.println("IdHousehold,IdPerson,TTCar,TTPT,DistanceCar,DistancePT");
		String line=reader.readLine();
		line=reader.readLine();
		int i=0;
		while(line!=null) {
			String[] parts=line.split(",");
			Coord start = coordinateTransformation.transform(new CoordImpl(parts[2], parts[3]));
			Coord end = coordinateTransformation.transform(new CoordImpl(parts[4], parts[5]));
			Path path = leastCostPathCalculator.calcLeastCostPath(((NetworkImpl)scenario2.getNetwork()).getNearestNode(start), ((NetworkImpl)scenario2.getNetwork()).getNearestNode(end), new Double(parts[6]), null, null);
			double distance = 0;
			for(Link link:path.links)
				distance+=link.getLength();
			List<Leg> legs = transitRouter.calcRoute(start, end, new Double(parts[6]), null);
			double distancePT = 0, timeCar = path.travelTime, timePT = 0;
			timeCar += (CoordUtils.calcDistance(start, ((NetworkImpl)scenario2.getNetwork()).getNearestNode(start).getCoord())+CoordUtils.calcDistance(end, ((NetworkImpl)scenario2.getNetwork()).getNearestNode(end).getCoord()))*3.6/4;
			if(legs!=null) {
				for(Leg leg:legs) {
					if(leg.getRoute() instanceof NetworkRoute)
						for(Id<Link> linkId:((NetworkRoute)leg.getRoute()).getLinkIds()) {
							distancePT+=scenario.getNetwork().getLinks().get(linkId).getLength();
							timePT += ttc.getLinkTravelTime(linkId, new Double(parts[6]));
						}
					else {
						timePT += leg.getTravelTime();
						if(leg.getRoute()!=null && leg.getRoute().getStartLinkId()!=null && leg.getRoute().getEndLinkId()!=null)
							distancePT+=CoordUtils.calcDistance(scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()).getCoord(), (scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getCoord()));
						else if(leg.getMode().equals("transit_walk"))
							distancePT+=4*leg.getTravelTime()/3.6;
						else
							System.out.println("Bad "+leg.getMode());
					}
				}
				writer.println(parts[0]+","+parts[1]+","+timeCar+","+timePT+","+distance+","+distancePT);
				if(++i%1000==0)
					System.out.println(i);
			}
			else
				System.out.println("No PT route for: "+parts[0]);
			line=reader.readLine();
		}
		reader.close();
		writer.close();
	}*/
	/*PT routing in congested network*/
	public static void main(String[] args) throws IOException {
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReaderV1(scenario).parse("./data/MATSim-Sin-2.0/input/transit/transitScheduleWAM.xml");
		TravelTimeCalculator ttc = new TravelTimeCalculator(scenario.getNetwork(), 15*60, 30*3600, scenario.getConfig().travelTimeCalculator());
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(ttc);
		new MatsimEventsReader(events).readFile("./data/MS2/output/ITERS/it.100/100.events.xml.gz");
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
		TransitRouter transitRouter = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig).get();
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/home_work_locations.csv")));
		PrintWriter writer = new PrintWriter(new File("./data/home_work_tt.csv"));
		writer.println("IdHousehold,IdPerson,TTCar,TTPT,DistanceCar,DistancePT");
		String line=reader.readLine();
		line=reader.readLine();
		int i=0;
		while(line!=null) {
			String[] parts=line.split(",");
			Coord start = coordinateTransformation.transform(new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
			Coord end = coordinateTransformation.transform(new Coord(Double.parseDouble(parts[4]), Double.parseDouble(parts[5])));
			double distance = 0;
			List<Leg> legs = transitRouter.calcRoute(start, end, new Double(parts[6]), null);
			double distancePT = 0, timePT = 0;
			if(legs!=null) {
				for(Leg leg:legs) {
					if(leg.getRoute() instanceof NetworkRoute)
						for(Id<Link> linkId:((NetworkRoute)leg.getRoute()).getLinkIds()) {
							distancePT+=scenario.getNetwork().getLinks().get(linkId).getLength();
							timePT += ttc.getLinkTravelTime(linkId, new Double(parts[6]));
						}
					else {
						timePT += leg.getTravelTime();
						if(leg.getRoute()!=null && leg.getRoute().getStartLinkId()!=null && leg.getRoute().getEndLinkId()!=null)
							distancePT+=CoordUtils.calcEuclideanDistance(scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()).getCoord(), (scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getCoord()));
						else if(leg.getMode().equals("transit_walk"))
							distancePT+=4*leg.getTravelTime()/3.6;
						else
							System.out.println("Bad "+leg.getMode());
					}
				}
				writer.println(parts[0]+","+parts[1]+","+timePT+","+distance+","+distancePT);
				if(++i%1000==0)
					System.out.println(i);
			}
			else
				System.out.println("No PT route for: "+parts[0]);
			line=reader.readLine();
		}
		reader.close();
		writer.close();
	}

}
