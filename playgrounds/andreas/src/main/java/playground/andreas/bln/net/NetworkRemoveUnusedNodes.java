package playground.andreas.bln.net;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;

/**
 * Nodes with no incoming or outgoing links are removed from the network.
 *
 * @author aneumann based on mrieser and balmermi
 */
public class NetworkRemoveUnusedNodes implements NetworkRunnable {

	private static final Logger log = Logger.getLogger(NetworkRemoveUnusedNodes.class);

	@Override
	public void run(final Network network) {
		log.info("running " + this.getClass().getName() + " algorithm...");
		log.info("  network contains " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");

		/* Reducing the network so it only contains nodes with links attached.
		 * Loop over all nodes and check if they have in or outLinks, if not, remove them from the network
		 */
		List<Node> nodesList = new ArrayList<Node>(network.getNodes().values());
		for (Node node : nodesList) {
			if (node.getInLinks().size() + node.getOutLinks().size() == 0) {
				network.removeNode(node.getId());		// removeNode takes care of removing links too in the network
			}
		}
		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");
		log.info("done.");
	}

}
