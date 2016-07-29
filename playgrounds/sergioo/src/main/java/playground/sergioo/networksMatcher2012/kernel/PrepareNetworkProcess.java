package playground.sergioo.networksMatcher2012.kernel;

import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import playground.sergioo.networksMatcher2012.kernel.core.ComposedLink;
import playground.sergioo.networksMatcher2012.kernel.core.ComposedNode;
import playground.sergioo.networksMatcher2012.kernel.core.MatchingComposedLink;
import playground.sergioo.networksMatcher2012.kernel.core.NetworksStep;

public class PrepareNetworkProcess extends NetworksStep {

	
	//Attributes

	private Set<String> modes;
	
	//Methods
	
	public PrepareNetworkProcess() {
		super("Prepare network", new InfiniteRegion());
	}

	public PrepareNetworkProcess(Set<String> modes) {
		super("Prepare network", new InfiniteRegion());
		this.modes = modes;
	}
	
	@Override
	protected void process(Network oldNetworkA, Network oldNetworkB) {
		networkA = convert(networkA);
		networkB = convert(networkB);
	}
	
	private Network convert(Network oldNetwork) {
		Network network = NetworkUtils.createNetwork();
		for(Node node:oldNetwork.getNodes().values())
			network.addNode(new ComposedNode(node));
		for(Link link:oldNetwork.getLinks().values()) {
			boolean withNeededMode = modes == null;
			if(!withNeededMode)
				for(String mode:modes)
					if(link.getAllowedModes().contains(mode))
						withNeededMode = true;
			if(withNeededMode) {
				ComposedLink composedLink = new MatchingComposedLink(link, network.getNodes().get(link.getFromNode().getId()), network.getNodes().get(link.getToNode().getId()), network);
				network.addLink(composedLink);
			}
		}
		for(Node node:network.getNodes().values()) {
			((ComposedNode)node).setAnglesDeviation();
			((ComposedNode)node).setType();
		}
		return network;
	}

	
}
