/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning;

import org.matsim.core.controler.Controler;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerModule;
import playground.thibautd.jointtripsoptimizer.replanning.selectors.PlanWithLongestTypeSelector;

/**
 * a {@link JointPlanStrategy} using a {@link JointPlanOptimizerModule}.
 * The plan to modify is selected using a {@link PlanWithLongestTypeSelector}
 *
 * @author thibautd
 */
public class ReplanningStrategy extends JointPlanStrategy {

	public ReplanningStrategy(final Controler controler) {
		this.planSelector = new PlanWithLongestTypeSelector();

		this.addStrategyModule(new JointPlanOptimizerModule(controler));
	}
}

