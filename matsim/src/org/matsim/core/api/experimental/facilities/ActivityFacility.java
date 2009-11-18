package org.matsim.core.api.experimental.facilities;

import java.util.TreeMap;

import org.matsim.core.facilities.ActivityOptionImpl;

public interface ActivityFacility extends Facility {

	public TreeMap<String, ActivityOptionImpl> getActivityOptions();

}