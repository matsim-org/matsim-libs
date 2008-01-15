package playground.andreas.intersection.sim;

import java.util.Iterator;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class QNetworkLayer extends NetworkLayer {

	@Override
	protected Node newNode(final String id, final String x, final String y, final String type) {
		return new QNode(id, x, y, type);
	}

	@Override
	protected Link newLink(final NetworkLayer network, final String id, final Node from, final Node to,
			final String length, final String freespeed, final String capacity, final String permlanes,
			final String origid, final String type) {
		return new QLink(this, id, from, to, length, freespeed, capacity, permlanes, origid, type);
	}

	/** Called by QSim.doSimStep */
	public void moveLinks(double now) {

		for (Iterator iter = locations.values().iterator(); iter.hasNext();) {
			QLink link = (QLink) iter.next();

			link.moveLink(now);
		}
	}

	/** Called by QSim.doSimStep */
	public void moveNodes(double now) {

		for (Iterator iter = nodes.values().iterator(); iter.hasNext();) {
			QNode node = (QNode) iter.next();

			node.moveNode(now);
		}
	}

}
