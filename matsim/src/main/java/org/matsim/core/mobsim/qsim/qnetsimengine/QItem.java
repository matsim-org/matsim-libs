package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.vis.snapshotwriters.VisVehicle;

/**
 * @author nagel
 */
interface QItem extends VisVehicle {
	
	double getEarliestLinkExitTime();

	void setEarliestLinkExitTime( double earliestLinkEndTime );
	
}
