package org.matsim.core.facilities;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.Facility;

public interface FacilityWRefs extends Facility {

	public Link getLink();

}