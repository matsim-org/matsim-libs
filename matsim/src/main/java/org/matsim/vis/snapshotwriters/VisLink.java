package org.matsim.vis.snapshotwriters;

import java.util.Collection;

import org.matsim.core.network.Link;

/**Interface that is meant to replace the direct "QueueLink" accesses in the visualizer
 * 
 * @author nagel
 *
 */
public interface VisLink {

	Link getLink() ;
	
	Collection<? extends VisVehicle> getAllVehicles() ;
	
	VisData getVisData() ;
}
