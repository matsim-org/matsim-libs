package org.matsim.core.basic.v01.facilities;

import org.matsim.api.basic.v01.BasicLocation;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Identifiable;

public interface BasicFacility extends BasicLocation, Identifiable {

	public Id getLinkId();

}