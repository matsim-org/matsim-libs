/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplanner.java
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

package playground.christoph.withinday.replanning;

import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.population.algorithms.PlanAlgorithm;

/*
 *	Marker Class
 */
public abstract class WithinDayReplanner {
	
	protected double time;
	protected DriverAgent driverAgent;
	protected PlanAlgorithm planAlgorithm;
	
	public abstract boolean doReplanning();
	
	public void setTime(double time)
	{
		this.time = time;
	}
	
	public void setDriverAgent(DriverAgent driverAgent)
	{
		this.driverAgent = driverAgent;
	}
	
	public void setReplanner(PlanAlgorithm planAlgorithm)
	{
		this.planAlgorithm = planAlgorithm;
	}
}
