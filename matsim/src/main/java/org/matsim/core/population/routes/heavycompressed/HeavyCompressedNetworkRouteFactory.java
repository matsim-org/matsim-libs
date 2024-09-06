package org.matsim.core.population.routes.heavycompressed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.SubsequentLinksAnalyzer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;

/**
 * @author mrieser / Simunto
 */
public class HeavyCompressedNetworkRouteFactory implements RouteFactory {

	private final Network network;
	private CompressionData compressionData = null;
	private final String preferredMode;

	/**
	 * Uses {@link SubsequentLinksAnalyzer} to get the map of subsequent links,
	 * used to compress the route information stored.
	 */
	public HeavyCompressedNetworkRouteFactory(Network network, String preferredMode) {
		this.network = network;
		this.preferredMode = preferredMode;
	}

	@Override
	public NetworkRoute createRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		if (this.network==null) {
			throw new RuntimeException( "need to set Network in Population in order to be able to create compressed routes") ;
		}
		if (this.compressionData == null) {
			Link[] subsequentLinks = new SubsequentLinksAnalyzer(this.network, this.preferredMode).getSubsequentLinks();
			this.compressionData = createCompressionData(this.network, subsequentLinks);
		}
		return new HeavyCompressedNetworkRoute(startLinkId, endLinkId, this.compressionData);
	}

	static CompressionData createCompressionData(Network network, Link[] subsequentLinks) {
		Link[] links = new Link[Id.getNumberOfIds(Link.class)];
		for (Link link : network.getLinks().values()) {
			links[link.getId().index()] = link;
		}
		return new CompressionData(links, subsequentLinks);
	}

	@Override
	public String getCreatedRouteType() {
		return "links";
	}

	record CompressionData(Link[] links, Link[] subsequentLinks) {
	}
}
