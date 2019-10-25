package playgroundMeng.travelTimeAnalysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator.Builder;
import org.matsim.vehicles.Vehicle;



public class travelTimeTest {
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setMaxTime(36*3600);
		builder.setTimeslice(100);
		  TravelTimeCalculator ttc = builder.build();
		  Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		  Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		  network.addNode(n1);
		  network.addNode(n2);
		  Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		  network.addLink(link1);
		  
		  Id<Vehicle> ivVehId1 = Id.create("ivVeh1", Vehicle.class);
		  Id<Vehicle> ivVehId2 = Id.create("ivVeh2", Vehicle.class);
		  
		  ttc.handleEvent(new LinkEnterEvent(50, ivVehId1, link1.getId()));
		  ttc.handleEvent(new LinkEnterEvent(110, ivVehId2, link1.getId()));


		  

		  
		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link1, 0, null, null));
		  System.out.println(ttc.getLinkTravelTimes().getLinkTravelTime(link1, 99, null, null));

		
	}

}
