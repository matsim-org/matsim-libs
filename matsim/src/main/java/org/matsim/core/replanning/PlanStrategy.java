/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimExtensionPoint;

/**
 * 
 * Examples of how this class can be used can be found are {@link
 * tutorial.programming.example10PluggablePlanStrategyFromFile.RunPluggablePlanStrategyFromFileExample} 
 * and {@link 
 *  tutorial.programming.example11PluggablePlanStrategyInCode.RunPluggablePlanStrategyInCodeExample}.
 * <br>
 *  * Notes:<ul>
 * <li> If an implementation of this interface is "innovative", i.e. modifies plans, then it should first copy that plan, add the new plan to the choice set,
 * and then modify that new copy.  Otherwise, the evolutionary functionality of MATSim will probably be destroyed. kai, jan'15
 * </ul>
 * 
 */
public interface PlanStrategy extends GenericPlanStrategy<Plan, Person>, MatsimExtensionPoint {


}