package org.matsim.core.api.experimental.facilities;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

public interface ActivityFacilities extends MatsimToplevelContainer {

	public Map<Id, ? extends ActivityFacility> getFacilities();

}