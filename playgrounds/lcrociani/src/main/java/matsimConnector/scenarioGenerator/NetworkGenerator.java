package matsimConnector.scenarioGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import matsimConnector.utility.Constants;
import matsimConnector.utility.Distances;
import matsimConnector.utility.MathUtility;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import pedCA.context.Context;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.FinalDestination;
import pedCA.environment.network.Coordinates;

public class NetworkGenerator {

	private static final Double DOOR_WIDTH = Constants.FAKE_LINK_WIDTH;
	private static final Double CA_LENGTH = Constants.CA_LINK_LENGTH;
	/*package*/ static double LINK_LENGTH = 10.;
	private static double FLOW = Constants.FLOPW_CAP_PER_METER_WIDTH * DOOR_WIDTH;
	private static Set<String> MODES = new HashSet<String>();
	static{
		MODES.add("walk"); //MODES.add("car");
	}
	
	protected static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create("n0",Node.class), new Coord(-20.,DOOR_WIDTH/2));
		Node n1 = fac.createNode(Id.create("n1",Node.class), new Coord(-10.,DOOR_WIDTH/2));
		Node n2 = fac.createNode(Id.create("n2",Node.class), new Coord(-0.2,DOOR_WIDTH/2));
		Node n5 = fac.createNode(Id.create("n5",Node.class), new Coord(CA_LENGTH+0.,DOOR_WIDTH/2));
		Node n6 = fac.createNode(Id.create("n6",Node.class), new Coord(CA_LENGTH+10.,DOOR_WIDTH/2));
		Node n7 = fac.createNode(Id.create("n7",Node.class), new Coord(CA_LENGTH+20.,DOOR_WIDTH/2));
		
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
		int nLinks = 2;
		int nodeCount = 0;
		int linkCount = 0;
		for (Destination dest : contextCA.getMarkerConfiguration().getDestinations()){
			if (dest instanceof FinalDestination){
				FinalDestination destinationCA = (FinalDestination) dest;
				ArrayList<Node> nodes = new ArrayList<Node>();
				for (int i=0; i<nLinks;i++){
					double x = destinationCA.getCoordinates().getX();
					double y = destinationCA.getCoordinates().getY();
					Coordinates coord = new Coordinates(x-(LINK_LENGTH*i)-0.2,y);
					MathUtility.rotate(coord, destinationCA.getRotation(), destinationCA.getCoordinates());
					nodes.add(fac.createNode(Id.create("n"+nodeCount,Node.class), new Coord(coord.getX(),coord.getY())));
					net.addNode(nodes.get(nodes.size()-1));
					nodeCount++;
				}			
				for (int i=1; i<nLinks;i++){
					Link linkOut = fac.createLink(Id.create("l"+linkCount,Link.class), nodes.get(i-1), nodes.get(i));
					Link linkIn = fac.createLink(Id.create("l"+(linkCount+1),Link.class), nodes.get(i), nodes.get(i-1));
					initLink(linkOut);
					initLink(linkIn);
					net.addLink(linkOut);
					net.addLink(linkIn);
					linkCount+=2;
				}
			}
		}
		
		Double x_min=null;
		Double y_min=null;
		Double x_max=null;
		Double y_max=null;
		boolean firstIt = true;		
		for (Node node : net.getNodes().values()){
			if (firstIt){
				x_min = node.getCoord().getX();
				x_max = node.getCoord().getX();
				y_min = node.getCoord().getY();
				y_max = node.getCoord().getY();
				firstIt = false;
			}
			else{
				if(node.getCoord().getY() < y_min)
					y_min = node.getCoord().getY();
				if(node.getCoord().getY() > y_max)
					y_max = node.getCoord().getY();
				if(node.getCoord().getX() < x_min)
					x_min = node.getCoord().getX();
				if(node.getCoord().getX() > x_max)
					x_max = node.getCoord().getX();
			}
		}
		ArrayList<Node> south = new ArrayList<Node>();
		ArrayList<Node> north = new ArrayList<Node>();
		ArrayList<Node> east = new ArrayList<Node>();
		ArrayList<Node> west = new ArrayList<Node>();
		for (Node node : net.getNodes().values()){
			if (node.getCoord().getY() == y_min && y_min < 0)
				south.add(node);
			if (node.getCoord().getY() == y_max && y_max > (double)contextCA.getRows()*Constants.CA_CELL_SIDE)
				north.add(node);
			if (node.getCoord().getX() == x_min && x_min < 0)
				west.add(node);
			if (node.getCoord().getX() == x_max && x_max > (double)contextCA.getColumns()*Constants.CA_CELL_SIDE)
				east.add(node);
		}
		if (south.size()>0){
			Coordinates centroid = Distances.centroid(south);
			Node orDestNode = fac.createNode(Id.create("n"+net.getNodes().size(),Node.class), new Coord(centroid.getX(),centroid.getY()-LINK_LENGTH));
			net.addNode(orDestNode);
			connect(orDestNode, south, net, fac,'s');
		
		}
		if (north.size()>0){
			Coordinates centroid = Distances.centroid(north);
			Node orDestNode = fac.createNode(Id.create("n"+net.getNodes().size(),Node.class), new Coord(centroid.getX(),centroid.getY()+LINK_LENGTH));
			net.addNode(orDestNode);
			connect(orDestNode, north, net, fac,'n');
		}
		if (west.size()>0){
			Coordinates centroid = Distances.centroid(west);
			Node orDestNode = fac.createNode(Id.create("n"+net.getNodes().size(),Node.class), new Coord(centroid.getX()-LINK_LENGTH,centroid.getY()));
			net.addNode(orDestNode);
			connect(orDestNode, west, net, fac, 'w');
		}
		if (east.size()>0){
			Coordinates centroid = Distances.centroid(east);
			Node orDestNode = fac.createNode(Id.create("n"+net.getNodes().size(),Node.class), new Coord(centroid.getX()+LINK_LENGTH,centroid.getY()));
			net.addNode(orDestNode);
			connect(orDestNode, east, net, fac, 'e');
		}
		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);		
	}

	private static void connect(Node orDestNode, ArrayList<Node> nodes, Network net, NetworkFactory fac, char direction) {
		for (Node node : nodes){
			Link linkOut = fac.createLink(Id.create("l"+net.getLinks().size(),Link.class), node, orDestNode);
			Link linkIn = fac.createLink(Id.create("l"+(net.getLinks().size()+1),Link.class), orDestNode, node);
			initLink(linkOut);
			initLink(linkIn);
			net.addLink(linkOut);
			net.addLink(linkIn);
		}
		Node firstNode = fac.createNode(Id.create("n_"+direction,Node.class), new Coord(orDestNode.getCoord().getX(),orDestNode.getCoord().getY()+LINK_LENGTH));
		Link linkOut = fac.createLink(Id.create("l"+net.getLinks().size(),Link.class), orDestNode, firstNode);
		Link linkIn = fac.createLink(Id.create("l"+(net.getLinks().size()+1),Link.class), firstNode, orDestNode);
		net.addNode(firstNode);
		initLink(linkOut);
		initLink(linkIn);
		net.addLink(linkOut);
		net.addLink(linkIn);
		
	}

	/*package*/ static void initLink(Link link) {
		link.setLength(LINK_LENGTH);
		link.setAllowedModes(MODES);
		link.setFreespeed(Constants.PEDESTRIAN_SPEED);
		link.setCapacity(FLOW);
		//link.setNumberOfLanes(LANES);
	}
	
}
