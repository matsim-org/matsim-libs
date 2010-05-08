package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.network.Link;

/**Interface that is meant to replace the direct "QueueLink" accesses in the visualizer
 * 
 * @author nagel
 *
 */
public interface VisualizableLink extends VisualizableObject {

	Link getLink() ;
	
}
