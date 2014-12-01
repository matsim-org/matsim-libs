package matsimConnector.scenarioGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import matsimConnector.utility.Constants;
import matsimConnector.utility.Distances;
import matsimConnector.utility.MathUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import pedCA.context.Context;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.TacticalDestination;
import pedCA.environment.network.Coordinates;

public class NetworkGenerator {

	private static final Double DOOR_WIDTH = Constants.FAKE_LINK_WIDTH;
	private static final Double CA_LENGTH = Constants.CA_LINK_LENGTH;
	
	protected static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create("n0",Node.class), new CoordImpl(-20.,DOOR_WIDTH/2));
		Node n1 = fac.createNode(Id.create("n1",Node.class), new CoordImpl(-10.,DOOR_WIDTH/2));
		Node n2 = fac.createNode(Id.create("n2",Node.class), new CoordImpl(-0.2,DOOR_WIDTH/2));
		Node n5 = fac.createNode(Id.create("n5",Node.class), new CoordImpl(CA_LENGTH+0.,DOOR_WIDTH/2));
		Node n6 = fac.createNode(Id.create("n6",Node.class), new CoordImpl(CA_LENGTH+10.,DOOR_WIDTH/2));
		Node n7 = fac.createNode(Id.create("n7",Node.class), new CoordImpl(CA_LENGTH+20.,DOOR_WIDTH/2));
		
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n5);
		net.addNode(n6);
		net.addNode(n7);
		
		double flow = Constants.FLOPW_CAP_PER_METER_WIDTH * DOOR_WIDTH;
		double lanes = DOOR_WIDTH/0.71;
		
		Link l0 = fac.createLink(Id.create("l0",Link.class), n0, n1);
		Link l1 = fac.createLink(Id.create("l1",Link.class), n1, n2);
		Link l2 = fac.createLink(Id.create("l2",Link.class), n5, n6);
		Link l3 = fac.createLink(Id.create("l3",Link.class), n6, n7);
		
		Link l0Rev = fac.createLink(Id.create("l0Rev",Link.class), n1, n0);
		Link l1Rev = fac.createLink(Id.create("l1Rev",Link.class), n2, n1);
		Link l2Rev = fac.createLink(Id.create("l2Rev",Link.class), n6, n5);
		Link l3Rev = fac.createLink(Id.create("l3Rev",Link.class), n7, n6);
		
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		l0.setLength(Distances.EuclideanDistance(l0));
		l1.setLength(Distances.EuclideanDistance(l1));
		l2.setLength(Distances.EuclideanDistance(l2));
		l3.setLength(Distances.EuclideanDistance(l3));
		
		l0Rev.setLength(Distances.EuclideanDistance(l0Rev));
		l1Rev.setLength(Distances.EuclideanDistance(l1Rev));
		l2Rev.setLength(Distances.EuclideanDistance(l2Rev));
		l3Rev.setLength(Distances.EuclideanDistance(l3Rev));
		
		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);
		
		l0.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l1.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l2.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l3.setFreespeed(Constants.PEDESTRIAN_SPEED);

		l0Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l1Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l2Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		l3Rev.setFreespeed(Constants.PEDESTRIAN_SPEED);
		
		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);
		
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);
		
		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);
		
		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		
		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}

	public static void createNetwork(Scenario sc, Context contextCA) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		int nLinks = 3;
		double linkLength = 10.;
		double flow = Constants.FLOPW_CAP_PER_METER_WIDTH * DOOR_WIDTH;
		double lanes = DOOR_WIDTH/0.71;
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		int nodeCount = 0;
		int linkCount = 0;
		for (Destination dest : contextCA.getMarkerConfiguration().getDestinations()){
			TacticalDestination destinationCA = (TacticalDestination) dest;
			ArrayList<Node> nodes = new ArrayList<Node>();
			for (int i=0; i<nLinks;i++){
				double x = destinationCA.getCoordinates().getX();
				double y = destinationCA.getCoordinates().getY();
				Coordinates coord = new Coordinates(x-(linkLength*i)-0.2,y);
				MathUtility.rotate(coord, destinationCA.getRotation(), destinationCA.getCoordinates());
				nodes.add(fac.createNode(Id.create("n"+nodeCount,Node.class), new CoordImpl(coord.getX(),coord.getY())));
				net.addNode(nodes.get(nodes.size()-1));
				nodeCount++;
			}			
			for (int i=1; i<nLinks;i++){
				Link linkOut = fac.createLink(Id.create("l"+linkCount,Link.class), nodes.get(i-1), nodes.get(i));
				Link linkIn = fac.createLink(Id.create("l"+(linkCount+1),Link.class), nodes.get(i), nodes.get(i-1));
				linkOut.setLength(linkLength);
				linkOut.setAllowedModes(modes);
				linkOut.setFreespeed(Constants.PEDESTRIAN_SPEED);
				linkOut.setCapacity(flow);
				linkOut.setNumberOfLanes(lanes);
				linkIn.setLength(linkLength);
				linkIn.setAllowedModes(modes);
				linkIn.setFreespeed(Constants.PEDESTRIAN_SPEED);
				linkIn.setCapacity(flow);
				linkIn.setNumberOfLanes(lanes);
				net.addLink(linkOut);
				net.addLink(linkIn);
				linkCount+=2;
			}
			
		}
		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);		
	}
	
}
