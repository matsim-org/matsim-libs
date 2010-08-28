package playground.kai.devmtg.mynetwork1;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;

public class MyNetwork extends NetworkImpl {

	private final static Logger log = Logger.getLogger("dummy");

//	@Override
//	public void addNode(final Node nn) {
//
//		if ( nn instanceof NodeImpl ) {
//			super.addNode( nn ) ;
//		} else {
//			Id id = nn.getId() ;
//			Node node = this.nodes.get(id);
//			if (node != null) {
//				if (node == nn) {
//					log.warn("Trying to add a node a second time to the network. node id = " + id.toString());
//					return;
//				}
//				throw new IllegalArgumentException("There exists already a node with id = " + id.toString() +
//						".\nExisting node: " + node + "\nNode to be added: " + node +
//						".\nNode is not added to the network.");
//			}
//			dosometghin();
//			this.nodes.put(id, nn);
//		}
//
//	}

	private void dosometghin() {
		throw new UnsupportedOperationException() ;
	}
}
