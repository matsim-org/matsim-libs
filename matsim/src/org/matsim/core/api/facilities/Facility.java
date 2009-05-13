package org.matsim.core.api.facilities;

import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.facilities.BasicFacility;

public interface Facility extends BasicFacility {

	public Link getLink();

}