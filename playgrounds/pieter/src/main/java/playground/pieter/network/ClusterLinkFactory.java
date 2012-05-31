package playground.pieter.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactory;

public class ClusterLinkFactory implements LinkFactory {

	@Override
	public Link createLink(Id id, Node from, Node to, Network network,
			double length, double freespeed, double capacity, double nOfLanes) {
		// TODO Auto-generated method stub
		return new ClusterLink(id, from, to, network, length, freespeed, capacity, nOfLanes);
	}

}
