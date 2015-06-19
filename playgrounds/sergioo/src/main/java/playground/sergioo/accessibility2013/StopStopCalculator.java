package playground.sergioo.accessibility2013;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkNode;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeStuckCalculator;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StopStopCalculator extends Thread{

	private static class RouteInfo {
		private Double travelTime;
		private Double firstWalkingDistance = 0.0;
		private Double lastWalkingDistance = 0.0;
		private Double firstWaitingTime;
		private Double numberOfTransfers = 0.0;
		private Double numberOfWalkingTransfers = 0.0;
		private Double sumWalkingDistances = 0.0;
		private Double sumWaitingTimes = 0.0;
	}
	private static class RouteInfo2 {
		private Double travelTime;
		private Double distance = 0.0;
		private Double numberOfTransfers = 0.0;
		private String transfers = ":";
		private String transfersM = ":";
		private String lines = ":";
		private String routes = ":";
	}
	private static final String S = ",";
	private TransitRouterNetworkWW network;
	private TransitRouterNetworkTravelTimeAndDisutilityWW travelFunction;
	private PreProcessDijkstra preProcessDijkstra;
	private int numberOfProcess;
	private double startTime;
	private double endTime;
	private int numProceses;
	private Scenario scenario;
	private double binSize;
	private String fileName;
	
	public StopStopCalculator(TransitRouterNetworkWW network,
			TransitRouterNetworkTravelTimeAndDisutilityWW travelFunction,
			PreProcessDijkstra preProcessDijkstra, int numberOfProcess,
			double startTime, double endTime, int numProceses,
			Scenario scenario, double binSize, String fileName) {
		super();
		this.network = network;
		this.travelFunction = travelFunction;
		this.preProcessDijkstra = preProcessDijkstra;
		this.numberOfProcess = numberOfProcess;
		this.startTime = startTime;
		this.endTime = endTime;
		this.numProceses = numProceses;
		this.scenario = scenario;
		this.binSize = binSize;
		this.fileName = fileName;
	}
	
	public void run2() {
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(network, travelFunction, travelFunction, preProcessDijkstra);
		double start = numberOfProcess*(endTime-startTime)/numProceses, end = (numberOfProcess+1)*(endTime-startTime)/numProceses;
		long milis = System.currentTimeMillis();
		Set<Node> toNodes = new HashSet<Node>();
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
			if(network.getNodes().get(stop.getId())!=null)
				toNodes.add(network.getNodes().get(stop.getId()));
		Writer writer = IOUtils.getBufferedWriter(numberOfProcess+"."+fileName);
		try {
			writer.write("time of the day"+S+
					"origin"+S+
					"destination"+S+
					"travel time"+S+
					"first walking distance"+S+
					"first waiting time"+S+
					"sum of walking distances"+S+
					"sum of waiting times"+S+
					"number of walking transfers"+S+
					"number of transfers"+S+
					"last walking distance\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(double time = start + binSize/2; time<end; time+=binSize) {
			System.out.println(time+": "+(System.currentTimeMillis()-milis));
			int s=0;
			for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
				if(network.getNodes().get(stop.getId())!=null) {
					System.out.println(s+++" "+time+" "+stop.getId()+": "+(System.currentTimeMillis()-milis));
					Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(network.getNodes().get(stop.getId()), toNodes, time, null, null);
					for(Entry<Id<Node>, Path> entry:paths.entrySet()) {
						Path path = entry.getValue();
						RouteInfo routeInfo = new RouteInfo();
						if(path!=null) {
							if(path.links.size()>0) {
								routeInfo.travelTime = path.travelTime;
								boolean first = true;
								double travelTime = 0;
								for(Link link:path.links) {
									TransitRouterNetworkLink tLink = (TransitRouterNetworkLink)link;
									TransitRouterNetworkNode tFromNode = (TransitRouterNetworkNode)link.getFromNode();
									TransitRouterNetworkNode tToNode = (TransitRouterNetworkNode)link.getToNode();
									if(first) {
										first = false;
										if(tToNode.getRoute()==null) {
											routeInfo.firstWalkingDistance += tLink.getLength();
											first = true;
										}
										else
											routeInfo.firstWaitingTime = travelFunction.getLinkTravelTime(link, startTime+travelTime, null, null);
									}
									else if(tLink.getRoute()==null)
										if(tToNode.getRoute()!=null) {
											routeInfo.sumWaitingTimes = travelFunction.getLinkTravelTime(link, startTime+travelTime, null, null);
											routeInfo.numberOfTransfers ++;
										}
										else if(tFromNode.getRoute()==null) {
											routeInfo.sumWalkingDistances += tLink.getLength();
											routeInfo.numberOfWalkingTransfers ++;
										}
									travelTime += travelFunction.getLinkTravelTime(link, startTime+travelTime, null, null);
								}
								if(((TransitRouterNetworkNode)path.links.get(path.links.size()-1).getFromNode()).getRoute()==null) {
									routeInfo.lastWalkingDistance = path.links.get(path.links.size()-1).getLength();
									routeInfo.sumWalkingDistances -= routeInfo.lastWalkingDistance;
									routeInfo.numberOfWalkingTransfers --;
								}
							}
						}
						else {
							routeInfo.travelTime = Double.POSITIVE_INFINITY;
							routeInfo.firstWalkingDistance = Double.NaN;
							routeInfo.firstWaitingTime = Double.NaN;
							routeInfo.sumWalkingDistances = Double.POSITIVE_INFINITY;
							routeInfo.sumWaitingTimes = Double.POSITIVE_INFINITY;
							routeInfo.numberOfTransfers = Double.NaN;
							routeInfo.numberOfWalkingTransfers = Double.NaN;
							routeInfo.lastWalkingDistance = Double.NaN;
						}
						try {
							writer.write(time+S+stop.getId()+S+entry.getKey()+S+
									routeInfo.travelTime+S+
									routeInfo.firstWalkingDistance+S+
									routeInfo.firstWaitingTime+S+
									routeInfo.sumWalkingDistances+S+
									routeInfo.sumWaitingTimes+S+
									routeInfo.numberOfWalkingTransfers+S+
									routeInfo.numberOfTransfers+S+
									routeInfo.lastWalkingDistance+"\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		//Between MRT transit stops
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(network, travelFunction, travelFunction, preProcessDijkstra);
		double start = numberOfProcess*(endTime-startTime)/numProceses, end = (numberOfProcess+1)*(endTime-startTime)/numProceses;
		long milis = System.currentTimeMillis();
		Set<Node> toNodes = new HashSet<Node>();
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
			if(network.getNodes().get(stop.getId())!=null)
				toNodes.add(network.getNodes().get(stop.getId()));
		Writer writer = IOUtils.getBufferedWriter(numberOfProcess+"."+fileName);
		try {
			writer.write("time of the day"+S+
					"origin"+S+
					"originM"+S+
					"destination"+S+
					"destinationM"+S+
					"travel time"+S+
					"distance"+S+
					"number of transfers"+S+
					"transfers"+S+
					"transfersM"+S+
					"lines"+S+
					"routes"+S+"\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(double time = start + binSize/2; time<end; time+=binSize) {
			System.out.println(time+": "+(System.currentTimeMillis()-milis));
			int s=0;
			Map<String, Tuple<String, Path>> finalPaths = new HashMap<String, Tuple<String, Path>>();
			for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
				if(network.getNodes().get(stop.getId())!=null) {
					System.out.println(s+++" "+time+" "+stop.getId()+": "+(System.currentTimeMillis()-milis));
					Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(network.getNodes().get(stop.getId()), toNodes, time, null, null);
					for(Entry<Id<Node>, Path> entry:paths.entrySet()) {
						Path path = entry.getValue();
						if(path!=null) {
							String origDest = ((TransitRouterNetworkNode)path.nodes.get(0)).getStop().getStopFacility().getName()+S+
									((TransitRouterNetworkNode)path.nodes.get(path.nodes.size()-1)).getStop().getStopFacility().getName();
							Tuple<String, Path> oldPathT = finalPaths.get(origDest);
							if(oldPathT==null || oldPathT.getSecond().travelCost>path.travelCost || (oldPathT.getSecond().travelCost==path.travelCost && oldPathT.getSecond().links.size()>path.links.size()))
								finalPaths.put(origDest, new Tuple<String, Path>(((TransitRouterNetworkNode)path.nodes.get(0)).getStop().getStopFacility().getId()+S+((TransitRouterNetworkNode)path.nodes.get(path.nodes.size()-1)).getStop().getStopFacility().getId(), path));
						}
					}
				}
			for(Entry<String, Tuple<String, Path>> pathE:finalPaths.entrySet()) {
				Path path = pathE.getValue().getSecond();
				RouteInfo2 routeInfo = new RouteInfo2();
				if(path!=null) {
					if(path.links.size()>0) {
						routeInfo.travelTime = path.travelTime;
						boolean first = true;
						double travelTime = 0;
						for(Link link:path.links) {
							TransitRouterNetworkLink tLink = (TransitRouterNetworkLink)link;
							TransitRouterNetworkNode tToNode = (TransitRouterNetworkNode)link.getToNode();
							if(first) {
								first = false;
								if(tToNode.getRoute()==null)
									first = true;
								else {
									routeInfo.lines+=tToNode.getLine().getId()+":";
									routeInfo.routes+=tToNode.getRoute().getId()+":";
								}
							}
							else if(tLink.getRoute()==null) {
								if(tToNode.getRoute()!=null) {
									routeInfo.transfers+=tToNode.stop.getStopFacility().getName()+":";
									routeInfo.transfersM+=tToNode.stop.getStopFacility().getId()+":";
									routeInfo.lines+=tToNode.getLine().getId()+":";
									routeInfo.routes+=tToNode.getRoute().getId()+":";
									routeInfo.numberOfTransfers ++;
								}
							}
							else
								routeInfo.distance += scenario.getNetwork().getLinks().get(tToNode.stop.getStopFacility().getLinkId()).getLength();
							travelTime += travelFunction.getLinkTravelTime(link, startTime+travelTime, null, null);
						}
						for(int i=5; i>routeInfo.numberOfTransfers; i--) {
							routeInfo.transfers+=":";
							routeInfo.transfersM+=":";
							routeInfo.lines+=":";
							routeInfo.routes+=":";
						}
					}
				}
				else {
					routeInfo.travelTime = Double.POSITIVE_INFINITY;
					routeInfo.distance = Double.POSITIVE_INFINITY;
					routeInfo.numberOfTransfers = Double.NaN;
				}
				try {
					writer.write(time+S+pathE.getKey()+S+
							pathE.getValue().getFirst()+S+
							routeInfo.travelTime+S+
							routeInfo.distance+S+
							routeInfo.numberOfTransfers+S+
							routeInfo.transfers+S+
							routeInfo.transfersM+S+
							routeInfo.lines+S+
							routeInfo.routes+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 * 0 - Configuration file
	 * 1 - Network file
	 * 2 - Population file
	 * 3 - Transit Schedule file
	 * 4 - Events file
	 * 5 - Start time
	 * 6 - End time
	 * 7 - Time bin
	 * 8 - Number of processes
	 * 9 - Output file
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]);
		int numProceses = new Integer(args[8]);
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetworkWW network = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.getBeelineWalkConnectionDistance());
		TransitRouterNetworkTravelTimeAndDisutilityWW travelFunction = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), network, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, new PreparedTransitSchedule(scenario.getTransitSchedule()));
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(network);
		for(int i=0; i<numProceses; i++)
			(new StopStopCalculator(network, travelFunction, preProcessDijkstra, i, startTime, endTime, numProceses, scenario, binSize, args[9])).start();
	}
	
	/**
	 * @param args
	 * 0 - Configuration file
	 * 1 - Network file
	 * 2 - Population file
	 * 3 - Transit Schedule file
	 * 4 - Events file
	 * 5 - Start time
	 * 6 - End time
	 * 7 - Time bin
	 * 8 - Number of processes
	 * 9 - Number of the process
	 * 10 - Output file
	 * @throws FileNotFoundException 
	 */
	/*public static void main2(String[] args) throws FileNotFoundException {
		Map<TimeOfDayOriginDestination, RouteInfo> travelTimes = new HashMap<TimeOfDayOriginDestination, RouteInfo>();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]), numProceses = new Double(args[8]), numberOfProcess = new Double(args[9]);
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetworkWW network = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkTravelTimeAndDisutilityVariableWW travelTime = new TransitRouterNetworkTravelTimeAndDisutilityVariableWW(transitRouterConfig, scenario.getNetwork(), network, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(network, travelTime, travelTime);
		double start = numberOfProcess*(endTime-startTime)/numProceses, end = (numberOfProcess+1)*(endTime-startTime)/numProceses;
		long milis = System.currentTimeMillis();
		for(double time = start + binSize/2; time<end; time+=binSize) {
			System.out.println(time+": "+(System.currentTimeMillis()-milis));
			for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
				System.out.println(stop.getId()+": "+(System.currentTimeMillis()-milis));
				for(TransitStopFacility stop2:scenario.getTransitSchedule().getFacilities().values())
					if(stop!=stop2) {
						Map<Node, InitialNode> fromNode = new HashMap<Node, MultiNodeDijkstra.InitialNode>();
						fromNode.put(network.getNodes().get(stop.getId()), new InitialNode(0, time));
						Map<Node, InitialNode> toNode = new HashMap<Node, MultiNodeDijkstra.InitialNode>();
						toNode.put(network.getNodes().get(stop2.getId()), new InitialNode(0, time));
						Path path = dijkstra.calcLeastCostPath(fromNode, toNode, null);
						RouteInfo routeInfo = new RouteInfo();
						if(path!=null) {
							routeInfo.time = path.travelTime;
							boolean first = true;
							for(Link link:path.links) {
								TransitRouterNetworkLink tLink = (TransitRouterNetworkLink)link;
								TransitRouterNetworkNode tFromNode = (TransitRouterNetworkNode)link.getFromNode();
								TransitRouterNetworkNode tToNode = (TransitRouterNetworkNode)link.getToNode();
								if(first) {
									first = false;
									if(tToNode.getRoute()==null) {
										routeInfo.firstWalkingDistance += tLink.getLength();
										first = true;
									}
									else
										routeInfo.firstWaitingTime = waitTimeCalculator.getWaitTimes().getRouteStopWaitTime(tToNode.getLine().getId(), tToNode.getRoute().getId(), tFromNode.getStop().getStopFacility().getId(), time);
								}
								else if(tLink.getRoute()==null)
									if(tToNode.getRoute()!=null) {
										routeInfo.sumWaitingTimes = waitTimeCalculator.getWaitTimes().getRouteStopWaitTime(tToNode.getLine().getId(), tToNode.getRoute().getId(), tFromNode.getStop().getStopFacility().getId(), time);
										routeInfo.numberOfTransfers ++;
									}
									else if(tFromNode.getRoute()==null) {
										routeInfo.sumWalkingDistances += tLink.getLength();
										routeInfo.numberOfWalkingTransfers ++;
									}
							}
							if(((TransitRouterNetworkNode)path.links.get(path.links.size()-1).getFromNode()).getRoute()==null) {
								routeInfo.lastWalkingDistance = path.links.get(path.links.size()-1).getLength();
								routeInfo.sumWalkingDistances -= routeInfo.lastWalkingDistance;
							}
						}
						else {
							routeInfo.time = Double.POSITIVE_INFINITY;
							routeInfo.firstWalkingDistance = Double.NaN;
							routeInfo.firstWaitingTime = Double.NaN;
							routeInfo.sumWalkingDistances = Double.POSITIVE_INFINITY;
							routeInfo.sumWaitingTimes = Double.POSITIVE_INFINITY;
							routeInfo.numberOfTransfers = Double.NaN;
							routeInfo.numberOfWalkingTransfers = Double.NaN;
							routeInfo.lastWalkingDistance = Double.NaN;
						}
						travelTimes.put(new TimeOfDayOriginDestination(time, stop.getId(), stop2.getId()), routeInfo);
					}
					else 
						travelTimes.put(new TimeOfDayOriginDestination(time, stop.getId(), stop2.getId()), new RouteInfo());
			}
		}
		PrintWriter writer = new PrintWriter(numberOfProcess+"."+args[10]);
		writer.println("time of the day"+S+
				"origin"+S+
				"destination"+S+
				"travel time"+S+
				"first walking distance"+S+
				"first waiting time"+S+
				"sum of walking distances"+S+
				"sum of waiting times"+S+
				"number of walking transfers"+S+
				"number of transfers"+S+
				"last walking distance");
		for(Entry<TimeOfDayOriginDestination, RouteInfo> entry:travelTimes.entrySet())
			writer.println(entry.getKey().time+S+
					entry.getKey().origin+S+
					entry.getKey().destination+S+
					entry.getValue().time+S+
					entry.getValue().firstWalkingDistance+S+
					entry.getValue().firstWaitingTime+S+
					entry.getValue().sumWalkingDistances+S+
					entry.getValue().sumWaitingTimes+S+
					entry.getValue().numberOfWalkingTransfers+S+
					entry.getValue().numberOfTransfers+S+
					entry.getValue().lastWalkingDistance);
		writer.close();
	}*/
	/**
	 * @param args
	 * 0 - Configuration file
	 * 1 - Network file
	 * 2 - Population file
	 * 3 - Transit Schedule file
	 * 4 - Events file
	 * 5 - Start time
	 * 6 - End time
	 * 7 - Time bin
	 * 8 - Number of processes
	 * 9 - Number of the process
	 * 10 - Output file
	 * @throws IOException 
	 */
	/*public static void main2(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]), numProceses = new Double(args[8]), numberOfProcess = new Double(args[9]);
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), (int)binSize, (int) (endTime-startTime));
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		(new EventsReaderXMLv1(eventsManager)).parse(args[4]);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
		TransitRouterNetworkWW network = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetworkTravelTimeAndDisutilityVariableWW travelFunction = new TransitRouterNetworkTravelTimeAndDisutilityVariableWW(transitRouterConfig, scenario.getNetwork(), network, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime);
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(network);
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(network, travelFunction, travelFunction, preProcessDijkstra);
		double start = numberOfProcess*(endTime-startTime)/numProceses, end = (numberOfProcess+1)*(endTime-startTime)/numProceses;
		long milis = System.currentTimeMillis();
		Writer writer = IOUtils.getBufferedWriter(numberOfProcess+"."+args[10]);
		writer.write("time of the day"+S+
				"origin"+S+
				"destination"+S+
				"travel time"+S+
				"first walking distance"+S+
				"first waiting time"+S+
				"sum of walking distances"+S+
				"sum of waiting times"+S+
				"number of walking transfers"+S+
				"number of transfers"+S+
				"last walking distance\n");
		for(double time = start + binSize/2; time<end; time+=binSize) {
			System.out.println(time+": "+(System.currentTimeMillis()-milis));
			Map<Id<TransitStopFacility>, Set<Id<TransitStopFacility>>> remainingPaths = new HashMap<Id<TransitStopFacility>, Set<Id<TransitStopFacility>>>();
			for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
				if(network.getNodes().get(stop.getId())!=null) {
					Set<Id<TransitStopFacility>> stopIds = new HashSet<Id<TransitStopFacility>>();
					for(TransitStopFacility stop2:scenario.getTransitSchedule().getFacilities().values())
						if(network.getNodes().get(stop2.getId())!=null) {
							stopIds.add(stop2.getId());
						}
					remainingPaths.put(stop.getId(), stopIds);
				}
			int s=0;
			for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
				if(network.getNodes().get(stop.getId())!=null) {
					System.out.println(s+++" "+time+" "+stop.getId()+": "+(System.currentTimeMillis()-milis));
					Set<Node> toNodes = new HashSet<Node>();
					for(Id<TransitStopFacility> stopId:remainingPaths.get(stop.getId()))
						if(network.getNodes().get(stopId)!=null)
							toNodes.add(network.getNodes().get(stopId));
					Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(network.getNodes().get(stop.getId()), toNodes, time, null, null);
					for(Entry<Id<Node>, Path> entry:paths.entrySet()) {
						Path mainPath = entry.getValue();
						if(mainPath!=null)
							if(mainPath.links.size()>0) {
								double cTravelTime = 0;
								for(int n = 1; n<mainPath.nodes.size()-1; n++) {
									TransitRouterNetworkNode tNode = (TransitRouterNetworkNode)mainPath.nodes.get(n);
									if(tNode.getRoute()==null) {
										for(int m = n+1; m<mainPath.nodes.size(); m++) {
											TransitRouterNetworkNode tNode2 = (TransitRouterNetworkNode)mainPath.nodes.get(m);
											if(tNode2.getRoute()==null)
												if(remainingPaths.get(tNode.stop.getStopFacility().getId()).contains(tNode2.stop.getStopFacility().getId())) {
													remainingPaths.get(tNode.stop.getStopFacility().getId()).remove(tNode2.stop.getStopFacility().getId());
													Path path = getPath(mainPath, n, m, time+cTravelTime, travelFunction);
													writePath(path, travelFunction, time+cTravelTime, tNode.getId(), tNode2.getId(), writer);
												}
										}
									}
									cTravelTime += travelFunction.getLinkTravelTime(mainPath.links.get(n), time+cTravelTime, null, null);
								}
							}
						else
							writer.write(time+S+stop.getId()+S+entry.getKey()+S+
									Double.POSITIVE_INFINITY+S+
									Double.NaN+S+
									Double.NaN+S+
									Double.POSITIVE_INFINITY+S+
									Double.POSITIVE_INFINITY+S+
									Double.NaN+S+
									Double.NaN+S+
									Double.NaN+"\n");
					}
				}
		}
		writer.close();
	}

	private static Path getPath(Path mainPath, int n, int m, double startTime,
			TransitRouterNetworkTravelTimeAndDisutilityVariableWW travelFunction) {
		List<Node> nodes = new ArrayList<Node>();
		List<Link> links = new ArrayList<Link>();
		double travelTime = 0;
		double travelCost = 0;
		for(int k=n; k<m; k++) {
			nodes.add(mainPath.nodes.get(k));
			Link link  = mainPath.links.get(k);
			links.add(link);
			travelCost += travelFunction.getLinkTravelDisutility(link, startTime+travelTime, null, null);
			travelTime += travelFunction.getLinkTravelTime(link, startTime+travelTime, null, null);
		}
		nodes.add(mainPath.nodes.get(m));
		return new Path(nodes, links, travelTime, travelCost);
	}*/

}
