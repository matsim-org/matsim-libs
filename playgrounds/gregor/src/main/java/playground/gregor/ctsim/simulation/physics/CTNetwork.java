package playground.gregor.ctsim.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.Map;

public class CTNetwork {

	
	private Map<Id<Link>, CTLink> links = new HashMap<>();
	private Map<Id<Node>, CTNode> nodes = new HashMap<>();
	
	private Network network;
	private EventsManager em;

	public CTNetwork(Network network, EventsManager em) {
		this.network = network;
		this.em = em;
		init();
	}

	private void init() {
		for (Node n : this.network.getNodes().values()) {
			CTNode ct = new CTNode(n.getId(), n);
			this.nodes.put(n.getId(),ct);
		}
		for (Link l : this.network.getLinks().values()) {
			if (links.get(l.getId()) != null) {
				continue;
			}
			Link rev = getRevLink(l);
			CTLink ct = new CTLink(l, rev, em, this, this.nodes.get(l.getFromNode().getId()), this.nodes.get(l.getToNode().getId()));
			links.put(l.getId(), ct);
			if (rev != null) {
				links.put(rev.getId(), ct);
			}
			
		}
		for (CTNode ctNode : this.nodes.values()) {
			ctNode.getCTCell().debug(em);
		}
	}

	private Link getRevLink(Link l) {
		for (Link rev : l.getToNode().getOutLinks().values()) {
			if (rev.getToNode() == l.getFromNode()) {
				return rev;
			}
		}
		return null;
	}

	CTNode getCTNode(Id<Node> id) {
		return this.nodes.get(id);
	}
}
