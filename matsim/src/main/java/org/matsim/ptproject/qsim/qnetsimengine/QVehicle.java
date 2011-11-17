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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;

/**
 * interface to the ``Q'' implementation of the MobsimVehicle.  This needs to be public since the ``Q'' version of the 
 * vehicle is used by more than one package.  This interfaces should, however, not be used outside the relevant 
 * netsimengines.  In particular, the information should not be used for visualization.  kai, nov'11
 * 
 * @author nagel
 */
@Deprecated // only makes sense for "queue" mobsims.  Should go somewhere else (I think).  kai, oct'10
public abstract class QVehicle extends QItem implements MobsimVehicle {

	public abstract void setCurrentLink(final Link link);
	// yy not sure if this needs to be publicly exposed
	
	/**Design thoughts:<ul>
	 * <li> I am fairly sure that this should not be publicly exposed.  As far as I can tell, it is used in order to 
	 * figure out of a visualizer should make a vehicle "green" or "red".  But green or red should be related to 
	 * vehicle speed, and the mobsim should figure that out, not the visualizer.  So something like "getCurrentSpeed" 
	 * seems to be a more useful option. kai, nov'11
	 * <li> The problem is not only the speed, but also the positioning of the vehicle in the "asQueue" plotting method.
	 * (Although, when thinking about it: Should be possible to obtain same result by using "getEarliestLinkExitTime()".
	 * But I am not sure if that would really be a conceptual improvement ... linkEnterTime is, after all, a much more
	 * "physical" quantity.)  kai, nov'11
	 * <li> Also see comment under setLinkEnterTime().  kai, nov'11 
	 * </ul>
	 */
	public abstract double getLinkEnterTime();
	
	/**Design thoughts:<ul>
	 * <li> This has to remain public as long as QVehicle/QVehicleImpl is both used by QueueSimulation and QSim.  At best,
	 * we could say that there should also be a MobsimVehicle interface that does not expose this.  kai, nov'11.
	 * (This is there now.  kai, nov'11)
	 * </ul>
	 */
	public abstract void setLinkEnterTime(final double time);

}
