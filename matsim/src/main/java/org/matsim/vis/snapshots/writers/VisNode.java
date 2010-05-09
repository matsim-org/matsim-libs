package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.network.Node;

/**Interface that is meant to replace the direct "QueueNode" accesses in the visualizer
 * 
 * @author nagel
 *
 */
public interface VisNode extends VisObject {

	Node getNode() ;
	
}
