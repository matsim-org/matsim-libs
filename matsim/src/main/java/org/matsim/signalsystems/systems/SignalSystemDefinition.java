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

package org.matsim.signalsystems.systems;

import org.matsim.api.core.v01.Id;
/**
 * 
 * @author dgrether
 *
 */
public interface SignalSystemDefinition {

	public Id getId();

	public Double getDefaultCycleTime();

	public void setDefaultCycleTime(Double defaultCirculationTime);

	public Double getDefaultSynchronizationOffset();

	public void setDefaultSynchronizationOffset(Double synchronizationOffset);

	public Double getDefaultInterGreenTime();

	public void setDefaultInterGreenTime(Double defaultInterimTime);

}