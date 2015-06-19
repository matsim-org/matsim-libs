/* *********************************************************************** *
 * project: org.matsim.*
 * SignalPlan
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
package org.matsim.contrib.signals.model;

import java.util.List;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public interface SignalPlan {

	public List<Id<SignalGroup>> getDroppings(double timeSeconds);

	public List<Id<SignalGroup>> getOnsets(double timeSeconds);

	public Double getEndTime();
	
	public Double getStartTime();

	public Id<SignalPlan> getId();
	
	public Integer getOffset();
	
	public Integer getCycleTime();
	
}
