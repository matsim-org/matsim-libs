package playgroundMeng.ptTravelTimeAnalysis.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator.Builder;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;



public class travelTimeTest {
	public static void main(String[] args) {
		
		Network network = NetworkUtils.createNetwork();
		Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setMaxTime(400);
		builder.setTimeslice(200);
		
		
		TravelTimeCalculator ttc = builder.build();
		
		
		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 500));
		Node n3 = network.getFactory().createNode(Id.create(3, Node.class), new Coord(1000, -500));
		Node n4 = network.getFactory().createNode(Id.create(4, Node.class), new Coord(2000, 0));
		  
		network.addNode(n1); network.addNode(n3);
		network.addNode(n2); network.addNode(n4);
		  
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		Link link2 = network.getFactory().createLink(Id.create(2, Link.class), n2, n4);
		Link link3 = network.getFactory().createLink(Id.create(3, Link.class), n4, n1);
		Link link4 = network.getFactory().createLink(Id.create(4, Link.class), n1, n3);
		Link link5 = network.getFactory().createLink(Id.create(5, Link.class), n3, n4);
		  
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		  
		Id<Vehicle> ivVehId1 = Id.create("1", Vehicle.class);
		Id<Person> personId = Id.create("1", Person.class);
		  
		Id<Vehicle> ivVehId2 = Id.create("2", Vehicle.class);
		Id<Person> personId2 = Id.create("2", Person.class);
		 
		ttc.handleEvent(new LinkEnterEvent(0, ivVehId1, link1.getId()));
		ttc.handleEvent(new LinkLeaveEvent(10, ivVehId1, link1.getId()));
		  
		ttc.handleEvent(new LinkEnterEvent(10, ivVehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(60, ivVehId1, link2.getId()));
		  
		ttc.handleEvent(new LinkEnterEvent(60, ivVehId1, link3.getId()));
		ttc.handleEvent(new LinkLeaveEvent(90, ivVehId1, link3.getId()));
		  
		ttc.handleEvent(new LinkEnterEvent(90, ivVehId1, link4.getId()));
		ttc.handleEvent(new LinkLeaveEvent(130, ivVehId1, link4.getId()));
		  
		ttc.handleEvent(new LinkEnterEvent(130, ivVehId1, link5.getId()));
		ttc.handleEvent(new LinkLeaveEvent(190, ivVehId1, link5.getId()));
		  
		ttc.handleEvent(new VehicleLeavesTrafficEvent(190, personId, link5.getId(), ivVehId1, "car", 1.0));
		  
		  // next
		  ttc.handleEvent(new LinkEnterEvent(200, ivVehId2, link1.getId()));
		  ttc.handleEvent(new LinkLeaveEvent(240, ivVehId2, link1.getId()));
		  
		  ttc.handleEvent(new LinkEnterEvent(240, ivVehId2, link2.getId()));
		  ttc.handleEvent(new LinkLeaveEvent(300, ivVehId2, link2.getId()));
		  
		  ttc.handleEvent(new LinkEnterEvent(300, ivVehId2, link3.getId()));
		  ttc.handleEvent(new LinkLeaveEvent(330, ivVehId2, link3.getId()));
		  
		  ttc.handleEvent(new LinkEnterEvent(330, ivVehId2, link4.getId()));
		  ttc.handleEvent(new LinkLeaveEvent(340, ivVehId2, link4.getId()));
		  
		  ttc.handleEvent(new LinkEnterEvent(340, ivVehId2, link5.getId()));
		  ttc.handleEvent(new LinkLeaveEvent(400, ivVehId2, link5.getId()));
		  
		  ttc.handleEvent(new VehicleLeavesTrafficEvent(390, personId2, link5.getId(), ivVehId2, "car", 1.0));
		 
		  


		  PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		  preProcessData.run(network);
		  TravelTime timeFunction = ttc.getLinkTravelTimes();
		  TravelDisutility costFunction = new OnlyTimeDependentTravelDisutility(timeFunction) ;
		  
		  DijkstraFactory dijkstraFactory = new DijkstraFactory();
		  LeastCostPathCalculator leastCostPathCalculator = dijkstraFactory.createPathCalculator(network, costFunction, timeFunction);
		 
		   Person person = null;
		   Vehicle vehicle = null;
		   
		Path path = leastCostPathCalculator.calcLeastCostPath(n1, n4, 230, person, vehicle);

		
		System.out.println(path.links);
		System.out.println(path.nodes);
		System.out.println(path.travelTime);
		  
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link1, 60, null, null));
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link1, 230, null, null));
//		  
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link2, 60, null, null));
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link2, 230, null, null));
//		  
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link3, 60, null, null));
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link3, 230, null, null));
//		  
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link4, 60, null, null));
//		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link4, 230, null, null));

		
	}

}
