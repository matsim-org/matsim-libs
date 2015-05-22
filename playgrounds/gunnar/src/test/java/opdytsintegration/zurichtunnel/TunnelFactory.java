package opdytsintegration.zurichtunnel;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TunnelFactory {

	Network network;

	final Set<Id<Link>> allLinkIds = new LinkedHashSet<Id<Link>>();

	TunnelFactory(final Network network) {
		this.network = network;
	}

	Tunnel newTunnel(String fromNodeName, String toNodeName, int lanes,
			double maxSpeed_km_h, final String link1Name,
			final String link2Name, final String tunnelName) {

		final Node fromNode = this.network.getNodes().get(
				Id.create(fromNodeName, Node.class));
		final Node toNode = this.network.getNodes().get(
				Id.create(toNodeName, Node.class));

		final Id<Link> linkId1 = Id.create(link1Name, Link.class);
		final Id<Link> linkId2 = Id.create(link2Name, Link.class);

		this.allLinkIds.add(linkId1);
		this.allLinkIds.add(linkId2);

		return new Tunnel(fromNode, toNode, lanes, maxSpeed_km_h, linkId1,
				linkId2, tunnelName);
	}

	void removeAllTunnels() {
		// System.out.println(">>>>> number of links BEFORE removeAllTunnels: " + this.network.getLinks().size());
		for (Id<Link> linkId : this.allLinkIds) {
			if (this.network.getLinks().containsKey(linkId)) {
				System.out.println("REMOVED LINK " + this.network.removeLink(linkId));
			}
		}
		// System.out.println(">>>>> number of links AFTER removeAllTunnels: " + this.network.getLinks().size());
	}

}
