/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.api.basic.v01.population;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;


/**
 *
 * @author dgrether
 */
public interface BasicActivity extends BasicPlanElement {

	public double getEndTime();

	public void setEndTime(final double seconds);

	/**
	 * Activity type is, until further notice, defined via the config file.
	 * 
	 * @return
	 */
	public String getType();

	public void setType(final String type);

	public Coord getCoord();

	public double getStartTime();
	// TODO kn not clear what this means.  I guess that in the same sense as "expected travel time" in leg I should accept it
	// as "expected activity start time".  --> remove for next version?

	public void setStartTime(double seconds);
	// TODO kn not clear what this means (see above). --> remove for next version?

	public Id getLinkId();

	public void setLinkId(final Id id);
	
	public Id getFacilityId();
	
	public void setFacilityId(final Id id);

}