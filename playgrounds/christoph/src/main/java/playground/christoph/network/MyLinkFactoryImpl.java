package playground.christoph.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkLayer;

public class MyLinkFactoryImpl implements LinkFactory{

	public MyLinkImpl createLink(Id id, Node from, Node to, NetworkLayer network, double length, double freespeedTravelTime, double capacity, double nOfLanes)
	{
		return new MyLinkImpl(id, from, to, network, length, freespeedTravelTime, capacity, nOfLanes);
	}
}
