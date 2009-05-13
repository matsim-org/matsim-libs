package org.matsim.core.api.facilities;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.facilities.BasicFacilities;

public interface Facilities extends BasicFacilities {

	public Map<Id, ? extends Facility> getFacilities();

}