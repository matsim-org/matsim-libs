package playground.sergioo.NetworksMatcher.kernel;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;


public interface MatchingAlgorithm {


	//Methods

	public Collection<NodesMatching> execute(Network networkA, Network networkB);

	public Collection<NodesMatching> execute(Network networkA, Network networkB, Region region);


}
