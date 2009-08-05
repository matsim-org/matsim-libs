package playground.christoph.network;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

public class MyLinkFactoryImpl implements LinkFactory{

	public MyLinkImpl createLink(Id id, NodeImpl from, NodeImpl to, NetworkLayer network, double length, double freespeedTravelTime, double capacity, double nOfLanes)
	{
		return new MyLinkImpl(id, from, to, (NetworkLayer) network, length, freespeedTravelTime, capacity, nOfLanes);
	}
}
