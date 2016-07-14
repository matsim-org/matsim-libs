package playground.sergioo.bestTravelTimeRouter2012;


import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.bestTravelTimeRouter2012.BestTravelTimePathCalculator.Path;

public class TestBestTravelTimeRouter {

	protected static final double MRT_SPEED = 60/3.6;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("./data/MATSim-Sin-2.0/input/transit/transitScheduleWAM.xml");
		NetworkFactory factory = new NetworkFactoryImpl(scenario.getNetwork());
		//Add travel links
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				if(route.getTransportMode().contains("subway") && route.getId().toString().split("_").length==3) {
					String r = route.getId().toString().split("_")[0];
					Node p = null;
					for(int l=0; l<route.getRoute().getLinkIds().size(); l++) {
						Node c = null;
						String[] od = route.getRoute().getLinkIds().get(l).toString().split("_");
						String[] os = od[0].split("/");
						String[] ds = od[1].split("/");
						for(String o:os)
							for(String d:ds)
								if(o.startsWith(r)&&d.startsWith(r)) {
									if(p==null) {
										Id<Node> id = Id.createNodeId(o);
										p = scenario.getNetwork().getNodes().get(id); 
										if(p==null) {
											p = factory.createNode(id, scenario.getTransitSchedule().getFacilities().get(id).getCoord());
											scenario.getNetwork().addNode(p);
										}
									}
									Id<Node> id = Id.createNodeId(d);
									c = scenario.getNetwork().getNodes().get(id); 
									if(c==null) {
										c = factory.createNode(id, scenario.getTransitSchedule().getFacilities().get(id).getCoord());
										scenario.getNetwork().addNode(c);
									}
									scenario.getNetwork().addLink(factory.createLink(Id.createLinkId(o+"_"+d), p, c));
								}
						p = c;
					}
					Node c = null;
					String[] od = route.getRoute().getEndLinkId().toString().split("_");
					String[] os = od[0].split("/");
					String[] ds = od[1].split("/");
					for(String o:os)
						for(String d:ds)
							if(o.startsWith(r)&&d.startsWith(r)) {
								if(p==null) {
									Id<Node> id = Id.createNodeId(o);
									p = scenario.getNetwork().getNodes().get(id); 
									if(p==null) {
										p = factory.createNode(id, scenario.getTransitSchedule().getFacilities().get(id).getCoord());
										scenario.getNetwork().addNode(p);
									}
								}
								Id<Node> id = Id.createNodeId(d);
								c = scenario.getNetwork().getNodes().get(id); 
								if(c==null) {
									c = factory.createNode(id, scenario.getTransitSchedule().getFacilities().get(id).getCoord());
									scenario.getNetwork().addNode(c);
								}
								scenario.getNetwork().addLink(factory.createLink(Id.createLinkId(o+"_"+d), p, c));
							}
					p = c;
				}
		//Add transfer links
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				if(route.getTransportMode().contains("subway") && route.getId().toString().split("_").length==3) {
					for(int l=0; l<route.getRoute().getLinkIds().size(); l++) {
						String[] od = route.getRoute().getLinkIds().get(l).toString().split("_");
						String[] ds = od[1].split("/");
						for(String d:ds)
							for(String d2:ds)
								if(d!=d2 && scenario.getNetwork().getLinks().get(Id.createLinkId(d+"T"+d2))==null)
									scenario.getNetwork().addLink(factory.createLink(Id.createLinkId(d+"T"+d2), scenario.getNetwork().getNodes().get(Id.createNodeId(d)), scenario.getNetwork().getNodes().get(Id.createNodeId(d2))));
					}
				}
		//Add origin nodes and links
		Collection<Node> newONodes = new ArrayList<Node>();
		Collection<Node> newDNodes = new ArrayList<Node>();
		for(Node n:scenario.getNetwork().getNodes().values()) {
			Node o = factory.createNode(Id.createNodeId("o_"+n.getId().toString()), n.getCoord());
			Node d = factory.createNode(Id.createNodeId(n.getId().toString()+"_d"), n.getCoord());
			newONodes.add(o);
			newDNodes.add(d);
		}
		/*for(Node o:newONodes) {
			Node n = scenario.getNetwork().getNodes().get(Id.createNodeId(o.getId().toString().split("_")[1])); 
			scenario.getNetwork().addNode(o);
			scenario.getNetwork().addLink(factory.createLink(Id.createLinkId("o_"+n.getId().toString()), o, n));
		}
		for(Node d:newDNodes) {
			Node n = scenario.getNetwork().getNodes().get(Id.createNodeId(d.getId().toString().split("_")[0])); 
			scenario.getNetwork().addNode(d);
			scenario.getNetwork().addLink(factory.createLink(Id.createLinkId(n.getId().toString()+"_d"), n, d));
		}*/
		//new SimpleNetworkWindow("MRT", scenario.getNetwork()).setVisible(true);
		final TravelTime tt = new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time,
					Person person, Vehicle vehicle) {
				//Origin link
				if(link.getId().toString().startsWith("o"))
					return 10*60+3*60;
				else if(link.getId().toString().endsWith("d"))
					return 10*60;
				//Travel link
				else if(link.getId().toString().contains("_"))
					return 30+link.getLength()/MRT_SPEED;
				//Tranfer link
				else
					return 15*60;
			}
		};			
		TravelDisutility travelDisutility = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return tt.getLinkTravelTime(link, time, person, vehicle);
			}			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		Node o = scenario.getNetwork().getNodes().get(Id.createNodeId("NS2"));
		Node d = scenario.getNetwork().getNodes().get(Id.createNodeId("NE15"));
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		long time = System.currentTimeMillis();
		LeastCostPathCalculator.Path shortestPath = new Dijkstra(scenario.getNetwork(), travelDisutility, tt, preProcessDijkstra).calcLeastCostPath(o, d, 0, null, null);
		System.out.println(System.currentTimeMillis()-time);
		System.out.println(shortestPath);
		System.out.println(shortestPath.travelTime);
		BestTravelTimePathCalculator bttpc = new BestTravelTimePathCalculatorImpl(scenario.getNetwork(), tt, false, preProcessDijkstra);
		time = System.currentTimeMillis();
		Path path = bttpc.calcBestTravelTimePath(o, d, 1700, 0);
		System.out.println(System.currentTimeMillis()-time);
		System.out.println(path);
		System.out.println(path.travelTime);
		time = System.currentTimeMillis();
		path = bttpc.calcBestTravelTimePath(o, d, 2050, 0);
		System.out.println(System.currentTimeMillis()-time);
		System.out.println(path);
		System.out.println(path.travelTime);
		time = System.currentTimeMillis();
		path = bttpc.calcBestTravelTimePath(o, d, 3000, 0);
		System.out.println(System.currentTimeMillis()-time);
		System.out.println(path);
		System.out.println(path.travelTime);
	}

}
