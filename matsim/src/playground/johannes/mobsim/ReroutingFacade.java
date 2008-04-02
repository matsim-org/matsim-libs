/* *********************************************************************** *
 * project: org.matsim.*
 * ReroutingFacade.java
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

import org.matsim.network.Link;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;

/**
 * @author illenberger
 *
 */
public abstract class ReroutingFacade implements PlanStrategy {

	protected abstract Route getRoute(Link origin, Link destination, double time);
	
	public Plan replan(double time) {
		Plan newPlan = null;
		Route route = getRoute(null, null, 0);
		//TODO: adpat new route...
		return newPlan;
	}

}
