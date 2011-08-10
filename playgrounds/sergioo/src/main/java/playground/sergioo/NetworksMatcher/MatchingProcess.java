package playground.sergioo.NetworksMatcher;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
		for(MatchingStep step:matchingSteps) {
			Network[] networks = step.execute(networkA, networkB);
			networkA = networks[0];
			networkB = networks[1];
		}
		finalNetworkA = networkA;
		finalNetworkB = networkB;
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
					System.out.println();//emptyLink.applyProperties(fullLink);
	}


}
