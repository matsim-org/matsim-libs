package org.matsim.core.mobsim.dsim;

import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;

public interface DistributedActivityHandler extends ActivityHandler {

	/**
	 * Activity engines are sorted by their priority in descending order.
	 */
	default double priority() {
		return 0.0;
	}
	
}
