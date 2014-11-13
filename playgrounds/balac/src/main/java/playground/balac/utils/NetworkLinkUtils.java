package playground.balac.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;

public class NetworkLinkUtils {
	
	Network network;
	public NetworkLinkUtils(Network network) {
		
		this.network = network;		}
	
	public LinkImpl getClosestLink(Coord coord) {
				
		return (LinkImpl) NetworkUtils.getNearestLink(((NetworkImpl) this.network), coord);
		
				
		
	}

}
