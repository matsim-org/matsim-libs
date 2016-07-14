package playground.balac.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;

public class NetworkLinkUtils {
	
	NetworkImpl network;
	public NetworkLinkUtils(Network network) {
		
		this.network = (NetworkImpl) network;		}
	
	public Link getClosestLink(Coord coord) {
			
		return (Link) NetworkUtils.getNearestLink(((NetworkImpl) this.network), coord);

	}

}
