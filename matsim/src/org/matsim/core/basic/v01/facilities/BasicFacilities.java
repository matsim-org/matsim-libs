package org.matsim.core.basic.v01.facilities;

import java.util.Map;

import org.matsim.api.basic.v01.Id;

public interface BasicFacilities {

	public Map<Id, ? extends BasicFacility> getFacilities();

}