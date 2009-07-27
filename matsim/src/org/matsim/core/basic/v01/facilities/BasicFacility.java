package org.matsim.core.basic.v01.facilities;

import org.matsim.api.basic.v01.BasicLocation;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Identifiable;

/**
 * A (Basic)Facility is a (Basic)Location ("getCoord") with an Id ("getId") that is connected to a Link ("getLinkId").
 * 
 * @author nagel
 */
public interface BasicFacility extends BasicLocation, Identifiable {

	public Id getLinkId();

}