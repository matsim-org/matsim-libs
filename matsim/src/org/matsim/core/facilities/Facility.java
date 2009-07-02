package org.matsim.core.facilities;

import org.matsim.core.basic.v01.facilities.BasicFacility;
import org.matsim.core.network.LinkImpl;

public interface Facility extends BasicFacility {

	public LinkImpl getLink();

}