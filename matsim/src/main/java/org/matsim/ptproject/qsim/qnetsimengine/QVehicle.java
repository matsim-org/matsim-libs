/* *********************************************************************** *
 * project: org.matsim.*
 * SimVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.vis.snapshotwriters.VisVehicle;

@Deprecated // only makes sense for "queue" mobsims.  Should go somewhere else (I think).  kai, oct'10
public interface QVehicle extends Identifiable, VisVehicle, QItem {

	public void setDriver(final MobsimDriverAgent driver);
	// yy presumably, this should set DriverAgent
	
	public Link getCurrentLink();
	
	public void setCurrentLink(final Link link);
	// yy not sure if this needs to be publicly exposed
	
	public double getSizeInEquivalents();
	
	/**Design thoughts:<ul>
	 * <li> yy I am fairly sure that this should not be publicly exposed.  As far as I can tell, it is used in order to 
	 * figure out of a visualizer should make a vehicle "green" or "red".  But green or red should be related to 
	 * vehicle speed, and the mobsim should figure that out, not the visualizer.  So something like "getCurrentSpeed" 
	 * seems to be a more useful option. kai, nov'11 
	 * </ul>
	 */
	public double getLinkEnterTime();
	
	public void setLinkEnterTime(final double time);
	// yy not sure if this needs to be publicly exposed

}
