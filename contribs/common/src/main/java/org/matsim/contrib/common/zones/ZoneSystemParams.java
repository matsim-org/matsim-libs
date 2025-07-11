package org.matsim.contrib.common.zones;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;

/**
 * @author nkuehnel / MOIA
 */
public abstract class ZoneSystemParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public ZoneSystemParams(String paramSetName) {
		super(paramSetName);
	}
}
