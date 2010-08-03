package playground.christoph.withinday.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkLayer;

public class WithinDayLinkFactoryImpl implements LinkFactory{

	@Override
	public WithinDayLinkImpl createLink(Id id, Node from, Node to, NetworkLayer network, double length, double freespeedTravelTime, double capacity, double nOfLanes)
	{
		return new WithinDayLinkImpl(id, from, to, network, length, freespeedTravelTime, capacity, nOfLanes);
	}
}
