package org.matsim.contrib.drt.extension.prebooking;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

/**
 * This parameter set activates the use of the prebooking functionality for a
 * DRT mode.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DrtPrebookingParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public static final String SET_NAME = "prebooking";

	public DrtPrebookingParams() {
		super(SET_NAME);
	}
}
