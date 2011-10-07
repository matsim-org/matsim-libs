package playground.sergioo.NetworksMatcher.kernel;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;

import playground.sergioo.NetworksMatcher.kernel.core.MatchingStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;

public class ShortPathsMatchingStep extends MatchingStep {


	//Attributes

	private double radius;
	private Network originalNetworkA;
	private Network originalNetworkB;
	
	//Methods
	
	public ShortPathsMatchingStep(Set<NodesMatching> nodesMatchings, Network originalNetworkA, Network originalNetworkB, Region region, double radius) {
		super("Short path matching step", region);
		this.originalNetworkA = originalNetworkA;
		this.originalNetworkB = originalNetworkB;
		this.radius = radius;
		this.nodesMatchings = new HashSet<NodesMatching>(); 
		for(NodesMatching nodeMatching:nodesMatchings)
			this.nodesMatchings.add(nodeMatching);
		networkSteps.add(new NetworkSimplifyStep(nodesMatchings, region));
		internalStepPosition = 1;
	}

	@Override
	protected void process(Network oldNetworkA, Network oldNetworkB) {
		networkA = originalNetworkA;
		networkB = originalNetworkB;
	}


}
