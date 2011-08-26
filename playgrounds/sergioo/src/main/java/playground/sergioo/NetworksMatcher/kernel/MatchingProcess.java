package playground.sergioo.NetworksMatcher.kernel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import playground.sergioo.NetworksMatcher.kernel.ComposedNode.Types;


public class MatchingProcess {


	//Attributes

	private final List<MatchingStep> matchingSteps;

	private Network finalNetworkA;

	private Network finalNetworkB;

	
	//Methods

	public MatchingProcess() {
		matchingSteps = new ArrayList<MatchingStep>();
	}

	public void addStep(MatchingAlgorithm matchingAlgorithm) {
		matchingSteps.add(new MatchingStep(matchingAlgorithm));
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

	private Network createDirectedGraph(Network network) {
		Network directedGraph = NetworkImpl.createNetwork();
		for(Node node:network.getNodes().values()) {
			Set<Node> nodes = new HashSet<Node>();
			nodes.add(node);
			directedGraph.addNode(new ComposedNode(nodes));
		}
		for(Link link:network.getLinks().values()) {
			ComposedLink composedLink = new ComposedLink(link, directedGraph);
			composedLink.setFromNode(directedGraph.getNodes().get(composedLink.getFromNode().getId()));
			composedLink.setToNode(directedGraph.getNodes().get(composedLink.getToNode().getId()));
			composedLink.getLinks().add(link);
			directedGraph.addLink(composedLink);
		}
		setNodeTypes(directedGraph);
		directedGraph = nodeReductionProcess(directedGraph);
		return directedGraph;
	}
	
	private void setNodeTypes(Network directedGraph) {
		for(Node node:directedGraph.getNodes().values())
			((ComposedNode)node).setType();
	}

	private Network nodeReductionProcess(Network directedGraph) {
		Network reducedDirectedGraph = NetworkImpl.createNetwork();
		for(Node node:directedGraph.getNodes().values()) {
			ComposedNode newNode = new ComposedNode(((ComposedNode)node).getNodes());
			newNode.setType(((ComposedNode)node).getType());
			reducedDirectedGraph.addNode(newNode);
		}
		for(Link link:directedGraph.getLinks().values()) {
			ComposedLink composedLink = new ComposedLink(link, reducedDirectedGraph);
			composedLink.setFromNode(reducedDirectedGraph.getNodes().get(composedLink.getFromNode().getId()));
			composedLink.setToNode(reducedDirectedGraph.getNodes().get(composedLink.getToNode().getId()));
			composedLink.getLinks().addAll(((ComposedLink)link).getLinks());
			reducedDirectedGraph.addLink(composedLink);
		}
		for(Node node:directedGraph.getNodes().values())
			switch(((ComposedNode)node).getType()) {
			case EMPTY:
				reducedDirectedGraph.removeNode(node.getId());
				break;
			case ONE_WAY_PASS:
				ComposedLink firstNext = (ComposedLink) reducedDirectedGraph.getNodes().get(node.getId()).getOutLinks().values().iterator().next();
				ComposedLink firstPrevious = (ComposedLink) reducedDirectedGraph.getNodes().get(node.getId()).getInLinks().values().iterator().next();
				Node next=firstNext.getToNode();
				Node previous=firstPrevious.getFromNode();
				while(((ComposedNode)next).getType().equals(Types.ONE_WAY_PASS) && !next.equals(firstPrevious.getToNode()))
					next = next.getOutLinks().values().iterator().next().getToNode();
				while(((ComposedNode)previous).getType().equals(Types.ONE_WAY_PASS) && !previous.equals(firstNext.getFromNode()))
					previous = previous.getInLinks().values().iterator().next().getFromNode();
				if(!next.equals(previous)) {
					ComposedLink composedLink = new ComposedLink(firstNext.getId(), firstPrevious.getFromNode(), firstNext.getToNode(), reducedDirectedGraph);
					composedLink.getLinks().addAll(firstNext.getLinks());
					composedLink.getLinks().addAll(firstPrevious.getLinks());
					reducedDirectedGraph.removeNode(node.getId());
					reducedDirectedGraph.addLink(composedLink);
				}
				break;
			case TWO_WAY_PASS:
				Iterator<ComposedLink> outLinksIterator = (Iterator<ComposedLink>)reducedDirectedGraph.getNodes().get(node.getId()).getOutLinks().values().iterator();
				ComposedLink firstNextA = (ComposedLink) outLinksIterator.next();
				ComposedLink firstNextB = (ComposedLink) outLinksIterator.next();
				Node nextA=firstNextA.getToNode();
				Node previousA=firstNextA.getFromNode();
				Node nextB=firstNextB.getToNode();
				Node previousB=firstNextA.getFromNode();
				while(((ComposedNode)nextA).getType().equals(Types.TWO_WAY_PASS) && !nextA.equals(firstNextB.getFromNode())){
					Node oldNextA = nextA;
					outLinksIterator = (Iterator<ComposedLink>) nextA.getOutLinks().values().iterator();
					nextA = outLinksIterator.next().getToNode();
					if(nextA.equals(previousA))
						nextA = outLinksIterator.next().getToNode();
					previousA = oldNextA;
				}
				while(((ComposedNode)nextB).getType().equals(Types.TWO_WAY_PASS) && !nextB.equals(firstNextA.getFromNode())) {
					Node oldNextB = nextB;
					outLinksIterator = (Iterator<ComposedLink>) nextB.getOutLinks().values().iterator();
					nextB = outLinksIterator.next().getToNode();
					if(nextB.equals(previousB))
						nextB = outLinksIterator.next().getToNode();
					previousB = oldNextB;
				}
				if(!nextA.equals(nextB)) {
					Iterator<ComposedLink> inLinksIterator = (Iterator<ComposedLink>)reducedDirectedGraph.getNodes().get(node.getId()).getInLinks().values().iterator();
					ComposedLink firstPreviousA = (ComposedLink) inLinksIterator.next();
					ComposedLink firstPreviousB = (ComposedLink) inLinksIterator.next();
					ComposedLink composedLinkA = new ComposedLink(firstNextA.getId(), firstNextB.getToNode(), firstNextA.getToNode(), reducedDirectedGraph);
					composedLinkA.getLinks().addAll(firstNextA.getLinks());
					composedLinkA.getLinks().addAll((firstPreviousA.getFromNode().equals(firstNextA.getToNode())?firstPreviousB:firstPreviousA).getLinks());
					ComposedLink composedLinkB = new ComposedLink(firstNextB.getId(), firstNextA.getToNode(), firstNextB.getToNode(), reducedDirectedGraph);
					composedLinkB.getLinks().addAll(firstNextB.getLinks());
					composedLinkB.getLinks().addAll((firstPreviousB.getFromNode().equals(firstNextB.getToNode())?firstPreviousA:firstPreviousB).getLinks());
					reducedDirectedGraph.removeNode(node.getId());
					reducedDirectedGraph.addLink(composedLinkA);
					reducedDirectedGraph.addLink(composedLinkB);
				}
				break;
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
	
	public Set<NodesMatching> getMatchings(int stepNumber) {
		return matchingSteps.get(stepNumber).getNodesMatchings();
	}

	public Set<NodesMatching> getFinalMatchings() {
		if(matchingSteps.isEmpty())
			return new HashSet<NodesMatching>();
		else
			return matchingSteps.get(matchingSteps.size()-1).getNodesMatchings();
	}

	
}
