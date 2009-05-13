package org.matsim.core.api.facilities;

import java.util.Map;

import org.matsim.api.basic.v01.Id;

public interface Facilities {

	public Map<Id, ? extends Facility> getFacilities();

}