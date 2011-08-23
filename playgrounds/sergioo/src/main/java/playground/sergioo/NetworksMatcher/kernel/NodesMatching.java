package playground.sergioo.NetworksMatcher.kernel;

import org.matsim.api.core.v01.network.Network;


public class NodesMatching {


	//Attributes

	private final NetworkNode subNetworkNodeA;
	
	private final NetworkNode subNetworkNodeB;


	//Methods

	protected NodesMatching(Network subNetworkNodeA, Network subNetworkNodeB) {
		super();
		this.subNetworkNodeA = new NetworkNode(subNetworkNodeA);
		this.subNetworkNodeB = new NetworkNode(subNetworkNodeB);
	}

	public NetworkNode getSubNetworkNodeA() {
		return subNetworkNodeA;
	}

	public NetworkNode getSubNetworkNodeB() {
		return subNetworkNodeB;
	}


}
