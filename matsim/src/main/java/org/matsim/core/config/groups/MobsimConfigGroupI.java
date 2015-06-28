/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimConfigGroupI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;

/**Interface which is there so that some params that exist jointly in different mobsim config groups can
 * be passed as arguments via the same channel.  One could argue that this should be a "QueueMobsimConfigGroupI",
 * but in the interest of simplicity the 1-2 args that arguable only make sense for queue were included. 
 * @author nagel
 */
public interface MobsimConfigGroupI {

	public abstract double getStartTime();

	public abstract double getEndTime();

	public abstract double getTimeStepSize();

	public abstract double getSnapshotPeriod();

	public abstract double getFlowCapFactor();

	public abstract double getStorageCapFactor();

	public abstract double getStuckTime();

	public abstract boolean isRemoveStuckVehicles();

	public abstract SnapshotStyle getSnapshotStyle();

}