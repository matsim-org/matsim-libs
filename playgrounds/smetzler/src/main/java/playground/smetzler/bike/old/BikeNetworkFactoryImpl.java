package playground.smetzler.bike.old;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
//import org.matsim.core.network.TimeVariantLinkFactory;

public class BikeNetworkFactoryImpl implements NetworkFactory {
	
	private final static Logger log = Logger.getLogger(NetworkFactoryImpl.class);

	private LinkFactory linkFactory = null;
	

	private NetworkChangeEventFactory networkChangeEventFactory = new NetworkChangeEventFactoryImpl();

	final Network network;

	public BikeNetworkFactoryImpl(final Network network) {
		this.network = network;
		this.linkFactory = new LinkFactoryImpl();
	}

	
	// like in NetworkFactoryImpl
	@Override
	public NodeImpl createNode(final Id<Node> id, final Coord coord) {
		NodeImpl node = new NodeImpl(id);
		node.setCoord(coord) ;
		return node ;
	}

	
	@Override
	public Link createLink(Id<Link> id, Node fromNode, Node toNode) {
		return this.linkFactory.createLink(id, fromNode, toNode, this.network, 1.0, 1.0, 1.0, 1.0);
	}

	
	public Link createLink(final Id<Link> id, final Node from, final Node to,
			final NetworkImpl network, final double length, final double freespeedTT, final double capacity,
			final double lanes) {
		return this.linkFactory.createLink(id, from, to, network, length, freespeedTT, capacity, lanes);
	}


//	/**
//	 * @param time the time when the NetworkChangeEvent occurs
//	 * @return a new NetworkChangeEvent
//	 *
//	 * @see #setNetworkChangeEventFactory(NetworkChangeEventFactory)
//	 */
//	public NetworkChangeEvent createNetworkChangeEvent(double time) {
//		return this.networkChangeEventFactory.createNetworkChangeEvent(time);
//	}
	
	public void setLinkFactory(final LinkFactory factory) {
		this.linkFactory = factory;
	}

//	public void setNetworkChangeEventFactory(NetworkChangeEventFactory networkChangeEventFactory) {
//		this.networkChangeEventFactory = networkChangeEventFactory;
//	}
	
//	public boolean isTimeVariant() {
//		return (this.linkFactory instanceof TimeVariantLinkFactory);
//	}


}
