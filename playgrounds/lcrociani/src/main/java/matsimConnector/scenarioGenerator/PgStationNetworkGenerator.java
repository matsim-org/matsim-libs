package matsimConnector.scenarioGenerator;

import java.util.HashSet;
import java.util.Set;

import matsimConnector.utility.Constants;
import matsimConnector.utility.Distances;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

import pedCA.context.Context;

public class PgStationNetworkGenerator extends NetworkGenerator {
	private static double OD_LINK_LENGTH = 10.;
	private static double FLOW = 999999;
	private static Set<String> MODES = new HashSet<String>();
	static{
	
		MODES.add("walk"); MODES.add("car");
	}
	
	public static void createNetwork(Scenario sc, Context contextCA) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		
		//nodes for the q environments
		Node n1 = fac.createNode(Id.create("n1",Node.class), new Coord(76, -35));
		Node n2 = fac.createNode(Id.create("n2",Node.class), new Coord(50.7, 73.5));
		Node n3 = fac.createNode(Id.create("n3",Node.class), new Coord(30.2, 140.4));
		Node n4 = fac.createNode(Id.create("n4",Node.class), new Coord(199.7, 201.4));
		Node n5 = fac.createNode(Id.create("n5",Node.class), new Coord(153.8, 254.2));
		Node n6 = fac.createNode(Id.create("n6",Node.class), new Coord(50.6, 311.4));
		Node n7 = fac.createNode(Id.create("n7",Node.class), new Coord(-28.5, 235));
		Node n8 = fac.createNode(Id.create("n8",Node.class), new Coord(-3.8, 130.9));
		Node n9 = fac.createNode(Id.create("n9",Node.class), new Coord(-26.8, 7.3));
		
		//border nodes of the 2d pedestrian environment
		Node n10 = fac.createNode(Id.create("n10",Node.class), new Coord(76, 0));
		Node n11 = fac.createNode(Id.create("n11",Node.class), new Coord(100.8, 36.8));
		Node n12 = fac.createNode(Id.create("n12",Node.class), new Coord(57.6, 41.2));
		Node n13 = fac.createNode(Id.create("n13",Node.class), new Coord(4, 41.2));
		Node n14 = fac.createNode(Id.create("n14",Node.class), new Coord(0, 29.8));
		
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		net.addNode(n6);
		net.addNode(n7);
		net.addNode(n8);
		net.addNode(n9);
		net.addNode(n10);
		net.addNode(n11);
		net.addNode(n12);
		net.addNode(n13);
		net.addNode(n14);
		
		generateLinksBetweenNodes(n10,n1,net,fac);
		
		generateLinksBetweenNodes(n11,n4,net,fac);
		generateLinksBetweenNodes(n12,n2,net,fac);
		generateLinksBetweenNodes(n2,n3,net,fac);
		generateLinksBetweenNodes(n2,n4,net,fac);
		generateLinksBetweenNodes(n3,n5,net,fac);
		generateLinksBetweenNodes(n4,n5,net,fac);
		generateLinksBetweenNodes(n5,n6,net,fac);
		generateLinksBetweenNodes(n6,n7,net,fac);
		generateLinksBetweenNodes(n7,n8,net,fac);
		generateLinksBetweenNodes(n13,n8,net,fac);
		generateLinksBetweenNodes(n14,n9,net,fac);
		
		connect(n1, net, fac, 's');
		connect(n9,net,fac,'w');
	}
	
	private static void generateLinksBetweenNodes(Node node1, Node node2, Network net, NetworkFactory fac){
		Link linkOut = fac.createLink(Id.create("l"+net.getLinks().size(),Link.class), node1, node2);
		Link linkIn = fac.createLink(Id.create("l"+(net.getLinks().size()+1),Link.class), node2, node1);
		initLink(linkOut);
		initLink(linkIn);
		net.addLink(linkOut);
		net.addLink(linkIn);
	}
		
	private static void connect(Node orDestNode, Network net, NetworkFactory fac, char direction) {
		Node firstNode = fac.createNode(Id.create("n_"+direction,Node.class), new Coord(orDestNode.getCoord().getX(),orDestNode.getCoord().getY()-OD_LINK_LENGTH));
		Link linkOut = fac.createLink(Id.create("l"+net.getLinks().size(),Link.class), orDestNode, firstNode);
		Link linkIn = fac.createLink(Id.create("l"+(net.getLinks().size()+1),Link.class), firstNode, orDestNode);
		net.addNode(firstNode);
		initLink(linkOut);
		initLink(linkIn);
		net.addLink(linkOut);
		net.addLink(linkIn);		
	}
	
	/*package*/ static void initLink(Link link) {
		link.setLength(Distances.EuclideanDistance(link));
		link.setAllowedModes(MODES);
		link.setFreespeed(Constants.PEDESTRIAN_SPEED);
		link.setCapacity(FLOW);
		link.setNumberOfLanes(10);
	}
}
