package playground.sergioo.passivePlanning2012.core.scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioImpl;

import playground.sergioo.passivePlanning2012.core.network.ComposedLinkFactory;
import playground.sergioo.passivePlanning2012.core.network.ComposedNode;

public class ScenarioSimplerNetwork extends ScenarioImpl {
	
	//Attributes
	private final Map<String, Network> simplerNetworks = new HashMap<String, Network>();
	
	//Methods
	public ScenarioSimplerNetwork(Config config) {
		super(config);
		Set<String> modes = new HashSet<String>();
		modes.addAll(config.plansCalcRoute().getNetworkModes());
		for(String mode:modes)
			simplerNetworks.put(mode, NetworkUtils.createNetwork());
	}
	public Network getSimplerNetwork(String mode) {
		return simplerNetworks.get(mode);
	}
	public void createSimplerNetwork(String mode, Set<ComposedNode> mainNodes) {
		for(ComposedNode mainNode:mainNodes)
			simplerNetworks.get(mode).addNode(mainNode);
		LinkFactory linkFactory = new ComposedLinkFactory(this.getNetwork(), simplerNetworks.get(mode), mode);
		long linkId = mainNodes.size();
		for(ComposedNode nodeA:mainNodes)
			for(ComposedNode nodeB:mainNodes) {
				Link link = linkFactory.createLink(Id.createLinkId(++linkId), nodeA, nodeB, simplerNetworks.get(mode), 0, 0, 0, 0);
				if(link!=null)
					simplerNetworks.get(mode).addLink(link);
			}	
	}

}
