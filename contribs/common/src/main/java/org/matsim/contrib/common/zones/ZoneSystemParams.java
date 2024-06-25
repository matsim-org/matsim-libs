package org.matsim.contrib.common.zones;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nkuehnel / MOIA
 */
public abstract class ZoneSystemParams extends ReflectiveConfigGroup {
	public ZoneSystemParams(String paramSetName) {
		super(paramSetName);
	}
}
