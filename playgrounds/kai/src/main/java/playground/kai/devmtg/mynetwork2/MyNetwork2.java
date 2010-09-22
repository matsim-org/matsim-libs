package playground.kai.devmtg.mynetwork2;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

public class MyNetwork2 implements Network {

	Network delegate = NetworkImpl.createNetwork() ;

	@Override
	public void addLink(Link ll) {
		delegate.addLink(ll);
	}

	@Override
	public void addNode(Node nn) {
		delegate.addNode(nn);
	}

	@Override
	public double getCapacityPeriod() {
		return delegate.getCapacityPeriod();
	}

	@Override
	public double getEffectiveLaneWidth() {
		return delegate.getEffectiveLaneWidth();
	}

	@Override
	public NetworkFactory getFactory() {
		return delegate.getFactory();
	}

	@Override
	public Map<Id, ? extends Link> getLinks() {
		return delegate.getLinks();
	}

	@Override
	public Map<Id, ? extends Node> getNodes() {
		return delegate.getNodes();
	}

	@Override
	public Link removeLink(Id linkId) {
		return delegate.removeLink(linkId);
	}

	@Override
	public Node removeNode(Id nodeId) {
		return delegate.removeNode(nodeId);
	}

//	public Network getDelegate() { return delegate ; }

}
