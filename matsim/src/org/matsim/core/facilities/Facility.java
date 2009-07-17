package org.matsim.core.facilities;

import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.basic.v01.facilities.BasicFacility;

public interface Facility extends BasicFacility {

	public Link getLink();

}