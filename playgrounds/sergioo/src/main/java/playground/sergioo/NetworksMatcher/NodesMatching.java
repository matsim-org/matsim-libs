package playground.sergioo.NetworksMatcher;

import org.matsim.api.core.v01.network.Network;


public class NodesMatching {


	//Attributes

	private final NodeNetwork subNodeNetworkA;

	private final NodeNetwork subNodeNetworkB;


	//Methods

	protected NodesMatching(Network subNodeNetworkA, Network subNodeNetworkB) {
		super();
		this.subNodeNetworkA = new NodeNetwork(subNodeNetworkA);
		this.subNodeNetworkB = new NodeNetwork(subNodeNetworkB);
	}

	public NodeNetwork getSubNodeNetworkA() {
		return subNodeNetworkA;
	}

	public NodeNetwork getSubNodeNetworkB() {
		return subNodeNetworkB;
	}


}
