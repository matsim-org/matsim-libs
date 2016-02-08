package matsimConnector.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;
import matsimConnector.utility.IdUtility;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;

import pedCA.environment.network.CAEdge;
import pedCA.environment.network.CANode;

public class HybridNetworkBuilder {

	/**
	 * Creates a matsim Network from the CANetwork in the CAEnvironment input object
	 * */
	public static void buildNetwork(CAEnvironment environmentCA, CAScenario scenarioCA) {
		NetworkImpl net = (NetworkImpl) NetworkUtils.createNetwork();
		NetworkFactoryImpl fac = net.getFactory();
		environmentCA.setNetwork(net);
		
		//////THIS LIST CONTAINS ID OF LINKS THAT WILL NOT BE ADDED IN THE MATSIM NETWORK
		ArrayList<String> linkIdBlackList = new ArrayList<String>();
		//linkIdBlackList.add("HybridNode_53-->HybridNode_12");
		
		net.setCapacityPeriod(1);
		net.setEffectiveCellSize(.26);
		net.setEffectiveLaneWidth(.71);
		
		Set<String> modes = new HashSet<String>();
		modes.add(Constants.CAR_LINK_MODE);
		modes.add(Constants.WALK_LINK_MODE);
		modes.add(Constants.CA_LINK_MODE);
		
		for (CANode nodeCA : environmentCA.getCANetwork().getNodes()) {
			Id<Node> id = IdUtility.createNodeId(nodeCA.getId());
			double x = nodeCA.getCoordinates().getX();
			double y = nodeCA.getCoordinates().getY();
			Node node = fac.createNode(id, new Coord(x,y));		
			net.addNode(node);
		}

		for (CAEdge edgeCA : environmentCA.getCANetwork().getEdges()) {
			Id<Node> fromId = IdUtility.createNodeId(edgeCA.getN1().getId());
			Id<Node> toId = IdUtility.createNodeId(edgeCA.getN2().getId());
			Node from = net.getNodes().get(fromId);
			Node to = net.getNodes().get(toId);
				
			Id <Link> linkId = IdUtility.createLinkId(fromId, toId);
			if (!(linkIdBlackList.contains(linkId.toString()))){
				if (edgeCA.isStairs())
					Constants.stairsLinks.add(linkId.toString());
				Link link = fac.createLink(linkId, from, to);
				link.setLength(edgeCA.getLength());
				link.setFreespeed(Constants.PEDESTRIAN_SPEED);
				
				//TODO FIX THE FLOW CAPACITY
				double width = Constants.FAKE_LINK_WIDTH;
				//double lanes = width/net.getEffectiveLaneWidth();
				double cap = width*Constants.FLOPW_CAP_PER_METER_WIDTH;
				link.setCapacity(cap);
				link.setNumberOfLanes(1);
				link.setAllowedModes(modes);
				net.addLink(link);
				scenarioCA.mapLinkToEnvironment(link, environmentCA);
			}
		}
	}

}
