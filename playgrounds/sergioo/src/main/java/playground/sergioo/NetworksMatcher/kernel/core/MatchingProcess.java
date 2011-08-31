package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;



public class MatchingProcess {


	//Attributes

	private final List<MatchingStep> matchingSteps;

	private ComposedNetwork finalNetworkA;

	private ComposedNetwork finalNetworkB;

	
	//Methods

	public MatchingProcess() {
		matchingSteps = new ArrayList<MatchingStep>();
	}

	public void addMatchingStep(MatchingStep networksStep) {
		matchingSteps.add(networksStep);
	}

	public void execute(Network networkA, Network networkB) {
		ComposedNetwork cNetworkA = ComposedNetwork.convert(networkA);
		ComposedNetwork cNetworkB = ComposedNetwork.convert(networkB);
		for(NetworksStep step:matchingSteps) {
			ComposedNetwork[] networks = step.execute(cNetworkA, cNetworkB);
			cNetworkA = networks[0];
			cNetworkB = networks[1];
		}
		finalNetworkA = cNetworkA;
		finalNetworkB = cNetworkB;
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
