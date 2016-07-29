package playground.smetzler.bike.old;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkUtils;

public class BikeNetworkFactoryImpl implements NetworkFactory {
	
	private final static Logger log = Logger.getLogger(NetworkFactory.class);

	private LinkFactory linkFactory = null;
	
	final Network network;

	public BikeNetworkFactoryImpl(final Network network) {
		this.network = network;
		this.linkFactory = NetworkUtils.createLinkFactory();
	}

	
	// like in NetworkFactoryImpl
	@Override
	public Node createNode(final Id<Node> id, final Coord coord) {
		Node node = NetworkUtils.createNode(id);
		node.setCoord(coord) ;
		return node ;
	}

	
	@Override
	public Link createLink(Id<Link> id, Node fromNode, Node toNode) {
		return this.linkFactory.createLink(id, fromNode, toNode, this.network, 1.0, 1.0, 1.0, 1.0);
	}

	
	public Link createLink(final Id<Link> id, final Node from, final Node to,
			final Network network, final double length, final double freespeedTT, final double capacity,
			final double lanes) {
		return this.linkFactory.createLink(id, from, to, network, length, freespeedTT, capacity, lanes);
	}
	
	@Override
	public void setLinkFactory(final LinkFactory factory) {
		this.linkFactory = factory;
	}

}
