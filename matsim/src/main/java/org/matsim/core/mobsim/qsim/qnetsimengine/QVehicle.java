
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

public interface QVehicle extends QItem, MobsimVehicle {
	void setCurrentLink( Link link );
	
	void setDriver( DriverAgent driver );
	
	double getLinkEnterTime();
	
	void setLinkEnterTime( double linkEnterTime );
	
	double getMaximumVelocity();
	
	double getSizeInEquivalents();
}
