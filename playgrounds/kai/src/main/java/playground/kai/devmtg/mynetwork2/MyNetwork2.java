package playground.kai.devmtg.mynetwork2;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;

public class MyNetwork2 implements Network {
	
	Network delegate = NetworkImpl.createNetwork() ;

	public void addLink(Link ll) {
		delegate.addLink(ll);
	}

	public void addNode(Node nn) {
		delegate.addNode(nn);
	}

	public double getCapacityPeriod() {
		return delegate.getCapacityPeriod();
	}

	public double getEffectiveLaneWidth() {
		return delegate.getEffectiveLaneWidth();
	}

	public NetworkFactory getFactory() {
		return delegate.getFactory();
	}

	public Map<Id, ? extends Link> getLinks() {
		return delegate.getLinks();
	}

	public Map<Id, ? extends Node> getNodes() {
		return delegate.getNodes();
	}

	public Link removeLink(Id linkId) {
		return delegate.removeLink(linkId);
	}

	public Node removeNode(Id nodeId) {
		return delegate.removeNode(nodeId);
	}
	
//	public Network getDelegate() { return delegate ; }

}
