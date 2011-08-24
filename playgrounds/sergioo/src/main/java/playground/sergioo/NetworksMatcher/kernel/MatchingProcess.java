package playground.sergioo.NetworksMatcher.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;


public class MatchingProcess {


	//Attributes

	private final List<MatchingStep> matchingSteps;

	private Network finalNetworkA;

	private Network finalNetworkB;

	
	//Methods

	public MatchingProcess() {
		matchingSteps = new ArrayList<MatchingStep>();
	}

	public void addStep(MatchingStep step) {
		matchingSteps.add(step);
	}

	public void execute(Network networkA, Network networkB) {
		Network networkDA  = createDirectedGraph(networkA);
		Network networkDB  = createDirectedGraph(networkB);
		for(MatchingStep step:matchingSteps) {
			Network[] networks = step.execute(networkDA, networkDB);
			networkDA = networks[0];
			networkDB = networks[1];
		}
		finalNetworkA = networkDA;
		finalNetworkB = networkDB;
	}

	private Network createDirectedGraph(Network networkA) {
		Network directedGraph = NetworkImpl.createNetwork();
		for(Node node:networkA.getNodes().values()) {
			Network nodeNetwork = NetworkImpl.createNetwork();
			nodeNetwork.addNode(node);
			directedGraph.addNode(new NetworkNode(nodeNetwork));
		}
		for(Link link:networkA.getLinks().values()) {
			link.setFromNode(directedGraph.getNodes().get(link.getFromNode().getId()));
			directedGraph.addLink(new ComposedLink(link, directedGraph));
		}
		setNodeTypes(directedGraph);
		directedGraph = nodeReductionProcess(directedGraph);
		return null;
	}
	
	private void setNodeTypes(Network directedGraph) {
		for(Node node:directedGraph.getNodes().values())
			((NetworkNode)node).setType();	
	}

	private Network nodeReductionProcess(Network directedGraph) {
		Network reducedDirectedGraph = NetworkImpl.createNetwork();
		for(Node node:directedGraph.getNodes().values())
			reducedDirectedGraph.addNode(node);
		for(Link link:directedGraph.getLinks().values())
			reducedDirectedGraph.addLink(link);
		for(Node node:directedGraph.getNodes().values()) {
			NetworkNode networkNode = (NetworkNode)node;
			switch(networkNode.getType()) {
			case EMPTY:
				reducedDirectedGraph.removeNode(networkNode.getId());
				break;
			case ONE_WAY_PASS:
				
				break;
			case TWO_WAY_PASS:
				break;
			}
		}
		return reducedDirectedGraph;
	}

	public Network getFinalNetworkA() {
		return finalNetworkA;
	}

	public Network getFinalNetworkB() {
		return finalNetworkB;
	}

	public Network getNetworkA(int stepNumber) {
		return matchingSteps.get(stepNumber).getNetworkA();
	}

	public Network getNetworkB(int stepNumber) {
		return matchingSteps.get(stepNumber).getNetworkB();
	}

	public void applyProperties(boolean fromAtoB) {
		Network fullNetwork, emptyNetwork;
		fullNetwork = fromAtoB?finalNetworkA:finalNetworkB;
		emptyNetwork = fromAtoB?finalNetworkB:finalNetworkA;
		MatchingStep finalMatchingStep = matchingSteps.get(matchingSteps.size()-1);
		for(Link fullLink:fullNetwork.getLinks().values())
			for(Link emptyLink:emptyNetwork.getLinks().values())
				if(finalMatchingStep.isMatched(fullLink.getFromNode(),emptyLink.getFromNode()) && finalMatchingStep.isMatched(fullLink.getToNode(),emptyLink.getToNode()))
					((ComposedLink)emptyLink).applyProperties((ComposedLink)fullLink);
	}
	
	public Collection<NodesMatching> getMatchings(int stepNumber) {
		return matchingSteps.get(stepNumber).getNodesMatchings();
	}

	public Collection<NodesMatching> getFinalMatchings() {
		return matchingSteps.get(matchingSteps.size()-1).getNodesMatchings();
	}

	
}
