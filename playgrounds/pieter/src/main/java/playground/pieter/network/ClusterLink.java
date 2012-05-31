package playground.pieter.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;

public class ClusterLink extends LinkImpl{

	protected ClusterLink(Id id, Node from, Node to, Network network,
			double length, double freespeed, double capacity, double lanes) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
		// TODO Auto-generated constructor stub
	}

}
