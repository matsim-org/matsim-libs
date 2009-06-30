/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.signalsystems.basic;

import org.matsim.api.basic.v01.Id;
/**
 * 
 * @author dgrether
 *
 */
public interface BasicSignalSystemDefinition {

	public Id getId();

	public double getDefaultCycleTime();

	public void setDefaultCycleTime(double defaultCirculationTime);

	public double getDefaultSynchronizationOffset();

	public void setDefaultSynchronizationOffset(double synchronizationOffset);

	public double getDefaultInterGreenTime();

	public void setDefaultInterGreenTime(double defaultInterimTime);

}