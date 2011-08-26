package playground.sergioo.NetworksMatcher.kernel;

import java.util.Set;

import org.matsim.api.core.v01.network.Network;


public interface MatchingAlgorithm {


	//Methods

	public Set<NodesMatching> execute(Network networkA, Network networkB);

	public Set<NodesMatching> execute(Network networkA, Network networkB, Region region);


}
