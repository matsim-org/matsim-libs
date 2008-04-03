/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStrategy.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;

/**
 * @author illenberger
 *
 */
public abstract class IntradayStrategy {

	protected PlanAgent agent;
	
	public IntradayStrategy(PlanAgent agent) {
		this.agent = agent;
	}
	
	public abstract Plan replan(double time);
	
	protected void adaptRoute(Route route, Leg leg, int index, double time) {
		/*
		 * TODO: Need link-based route implementation here.
		 * TODO: Move this to re-routing strategy?
		 */
	}
	
}
