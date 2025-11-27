
/* *********************************************************************** *
 * project: org.matsim.*
 * QVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

public interface QVehicle extends QItem, MobsimVehicle {
	void setCurrentLinkId(Id<Link> link);

	void setDriver(DriverAgent driver);

	/**
	 * @deprecated I could not find any usages in the matsim libs repository. Therefore, this method can probably be removed after the next release. janek nov' 24
	 * According to a lengthy comment from Kai in 2012(!) He thought that this should no longer be exposed and that visualizers should figure out the
	 * coloring of moving vehicles otherwise.
	 */
	@Deprecated(since = "2024", forRemoval = true)
	double getLinkEnterTime();

	/**
	 * @deprecated I could not find any usages in the matsim libs repository. Therefore, this method can probably be removed after the next release. janek nov' 24
	 */
	@Deprecated(since = "2024", forRemoval = true)
	void setLinkEnterTime(double linkEnterTime);

	double getMaximumVelocity();

	double getSizeInEquivalents();
}
