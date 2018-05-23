package lsp.usecase;



import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;


import com.google.common.collect.Sets;

public class NetworkMaker {

	public static void main (String[]args){
		Network network = NetworkUtils.createNetwork();
	
		for(int i=0; i < 5; i++){
			for(int j = 0; j < 5; j++){
				Coord coord = new Coord(i,j);
				Id <Node> id = Id.createNodeId("("+i+" " +j+")");
				Node node = NetworkUtils.createNode(id, coord);
				network.addNode(node);
			}
		}
		
		for(int i=14; i < 19; i++){
			for(int j = 0; j < 5; j++){
				Coord coord = new Coord(i,j);
				Id <Node> id = Id.createNodeId("("+i+" " +j+")");
				Node node = NetworkUtils.createNode(id, coord);
				network.addNode(node);
			}
		}

		
		for(int i = 0; i < 4; i++){
			for( int j = 0; j < 5 ; j++){
				Id <Node> id1 = Id.createNodeId("("+i+" " +j+")");
				Node node1 = network.getNodes().get(id1);
				Id <Node> id2 = Id.createNodeId("("+(i+1)+" " +j+")");
				Node node2 = network.getNodes().get(id2);
				Link link1 = NetworkUtils.createLink(Id.createLinkId("("+i+" " +j+")" + " " + "("+(i+1)+" " +j+")"), node1, node2, network, 10000, 7.5, 10, 1);
				link1.setLength(1000);
				link1.setFreespeed(7.5);
				link1.setCapacity(10.0);
				link1.setNumberOfLanes(1.0);
				link1.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link1);
				Link link2 = NetworkUtils.createLink(Id.createLinkId("("+(i+1)+" " +j+")"   + " " +   "("+i+" " +j+")"), node2, node1, network, 10000, 7.5, 10, 1);	
				link2.setLength(1000);
				link2.setFreespeed(7.5);
				link2.setCapacity(10.0);
				link2.setNumberOfLanes(1.0);
				link2.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link2);
			}
			
		}
	
		for(int i = 0; i < 5; i++){
			for( int j = 0; j < 4 ; j++){
				Id <Node> id1 = Id.createNodeId("("+i+" " +j+")");
				Node node1 = network.getNodes().get(id1);
				Id <Node> id2 = Id.createNodeId("("+i+" " +(j+1)+")");
				Node node2 = network.getNodes().get(id2);
				Link link1 = NetworkUtils.createLink(Id.createLinkId("("+i+" " +j+")" + " " + "("+i+" " +(j+1)+")"), node1, node2, network, 10000, 7.5, 10, 1);
				link1.setLength(1000);
				link1.setFreespeed(7.5);
				link1.setCapacity(10.0);
				link1.setNumberOfLanes(1.0);
				link1.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link1);
				Link link2 = NetworkUtils.createLink(Id.createLinkId("("+i+" " +(j+1)+")"  + " " +   "("+i+" " +j+")"), node2, node1, network, 10000, 7.5, 10, 1);	
				link2.setLength(1000);
				link2.setFreespeed(7.5);
				link2.setCapacity(10.0);
				link2.setNumberOfLanes(1.0);
				link2.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link2);
			}
			
		}
	
		for(int i = 14; i < 18; i++){
			for( int j = 0; j < 5 ; j++){
				Id <Node> id1 = Id.createNodeId("("+i+" " +j+")");
				Node node1 = network.getNodes().get(id1);
				Id <Node> id2 = Id.createNodeId("("+(i+1)+" " +j+")");
				Node node2 = network.getNodes().get(id2);
				Link link1 = NetworkUtils.createLink(Id.createLinkId("("+i+" " +j+")" + " " + "("+(i+1)+" " +j+")"), node1, node2, network, 10000, 7.5, 10, 1);
				link1.setLength(1000);
				link1.setFreespeed(7.5);
				link1.setCapacity(10.0);
				link1.setNumberOfLanes(1.0);
				link1.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link1);
				Link link2 = NetworkUtils.createLink(Id.createLinkId("("+(i+1)+" " +j+")"   + " " +   "("+i+" " +j+")"), node2, node1, network, 10000, 7.5, 10, 1);	
				link2.setLength(1000);
				link2.setFreespeed(7.5);
				link2.setCapacity(10.0);
				link2.setNumberOfLanes(1.0);
				link2.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link2);
			}
			
		}
	
		for(int i = 14; i < 19; i++){
			for( int j = 0; j <4  ; j++){
				Id <Node> id1 = Id.createNodeId("("+i+" " +j+")");
				Node node1 = network.getNodes().get(id1);
				Id <Node> id2 = Id.createNodeId("("+i+" " +(j+1)+")");
				Node node2 = network.getNodes().get(id2);
				Link link1 = NetworkUtils.createLink(Id.createLinkId("("+i+" " +j+")" + " " + "("+i+" " +(j+1)+")"), node1, node2, network, 10000, 7.5, 10, 1);
				link1.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link1);
				Link link2 = NetworkUtils.createLink(Id.createLinkId("("+i+" " +(j+1)+")"  + " " +   "("+i+" " +j+")"), node2, node1, network, 10000, 7.5, 10, 1);	
				link2.setAllowedModes(Sets.newHashSet("car"));
				network.addLink(link2);
			}
			
		}
	
		Id <Node> id1 = Id.createNodeId("(4 2)");
		Node node1 = network.getNodes().get(id1);
		Id <Node> id2 = Id.createNodeId("(14 2)");
		Node node2 = network.getNodes().get(id2);
		Link link1 = NetworkUtils.createLink(Id.createLinkId("(4 2)" + " " + "(14  2)"), node1, node1, network, 10000, 7.5, 10, 1);
		link1.setAllowedModes(Sets.newHashSet("car"));
		network.addLink(link1);
		Link link2 = NetworkUtils.createLink(Id.createLinkId("(14 2)" + " " + "(4  2)"), node2, node1, network, 10000, 7.5, 10, 1);
		link2.setAllowedModes(Sets.newHashSet("car"));
		network.addLink(link2);
	
		NetworkWriter writer = new NetworkWriter(network);
		writer.write("D:/Transport_Chains/workspace_TransportChains/logistics/input/lsp/network/2regions.xml");
		
	}
	
	
	
	
}
