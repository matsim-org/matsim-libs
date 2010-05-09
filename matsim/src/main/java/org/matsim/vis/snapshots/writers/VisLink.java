package org.matsim.vis.snapshots.writers;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;

/**Interface that is meant to replace the direct "QueueLink" accesses in the visualizer
 * 
 * @author nagel
 *
 */
public interface VisLink extends VisObject {

	Link getLink() ;
	
	Collection<? extends VisVehicle> getAllVehicles() ;
}
