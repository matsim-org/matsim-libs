package org.matsim.contrib.map2mapmatching.kernel.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.map2mapmatching.kernel.PrepareNetworkProcess;



public class MatchingProcess {


	//Attributes

	private final List<NetworksStep> steps;

	private Network finalNetworkA;

	private Network finalNetworkB;

	
	//Methods

	public MatchingProcess() {
		steps = new ArrayList<NetworksStep>();
		steps.add(new PrepareNetworkProcess());
	}
	
	public MatchingProcess(Set<String> modes) {
		steps = new ArrayList<NetworksStep>();
		steps.add(new PrepareNetworkProcess(modes));
	}
	
	public void addMatchingStep(MatchingStep networksStep) {
		steps.add(networksStep);
	}

	public void execute(Network networkA, Network networkB) {
		for(NetworksStep step:steps) {
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
	
	public NetworksStep getStep(int stepNumber) {
		int[] pos = new int[]{0};
		for(NetworksStep step:steps) {
			NetworksStep nStep = getStep(step, pos, stepNumber);
			if(nStep!=null)
				return nStep;
		}
		return null;
	}

	private NetworksStep getStep(NetworksStep step, int[] pos, int stepNumber) {
		int k=0;
		for(NetworksStep subStep:step.getNetworkSteps()) {
			if(k==step.getInternalStepPosition()) {
				if(stepNumber==pos[0])
					return step;
				else
					pos[0]++;
			}
			NetworksStep nStep = getStep(subStep, pos, stepNumber);
			if(nStep!=null)
				return nStep;
			k++;
		}
		if(k==0 && stepNumber==pos[0])
			return step;
		else if(k==0)
			pos[0]++;
		else if(k==step.getInternalStepPosition()) {
			if(stepNumber==pos[0])
				return step;
			else
				pos[0]++;
		}
		return null;
	}
	
	public Network getNetworkA(int stepNumber) {
		return getStep(stepNumber).getNetworkA();
	}

	public Network getNetworkB(int stepNumber) {
		return getStep(stepNumber).getNetworkB();
	}

	public void applyProperties(boolean fromAtoB) {
		Network fullNetwork, emptyNetwork;
		fullNetwork = fromAtoB?finalNetworkA:finalNetworkB;
		emptyNetwork = fromAtoB?finalNetworkB:finalNetworkA;
		MatchingStep finalMatchingStep = (MatchingStep) steps.get(steps.size()-1);
		for(Link fullLink:fullNetwork.getLinks().values())
			for(Link emptyLink:emptyNetwork.getLinks().values())
				if(finalMatchingStep.isMatched(fullLink.getFromNode(),emptyLink.getFromNode()) && finalMatchingStep.isMatched(fullLink.getToNode(),emptyLink.getToNode()))
					((MatchingComposedLink)emptyLink).applyProperties((MatchingComposedLink)fullLink);
	}
	
	public Set<NodesMatching> getNodesMatchings(int stepNumber) {
		try {
			return ((MatchingStep)getStep(stepNumber)).getNodesMatchings();
		} catch (Exception e) {
			return null;
		}
	}
	
	public Set<NodesMatching> getFinalMatchings() {
		if(steps.isEmpty())
			return new HashSet<NodesMatching>();
		else
			return ((MatchingStep) steps.get(steps.size()-1)).getNodesMatchings();
	}

	public int getNumSteps() {
		int num = 0;
		for(NetworksStep step:steps) {
			num+=getNumSteps(step);
		}
		return num;
	}

	private int getNumSteps(NetworksStep step) {
		int num = 1;
		for(NetworksStep subStep:step.getNetworkSteps()) {
			num+=getNumSteps(subStep);
		}
		return num;
	}
	
	
}
