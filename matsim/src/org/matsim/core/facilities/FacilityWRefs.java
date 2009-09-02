package org.matsim.core.facilities;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.facilities.BasicFacility;

public interface FacilityWRefs extends BasicFacility {

	public Link getLink();

}