/* *********************************************************************** *
 * project: org.matsim.*
 * BestScoreStrategy.java
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
import org.matsim.core.replanning.selectors.BestPlanSelector;

/**
 * Wraps a BestPlanSelector into a JointPlanStrategy.
 * @author thibautd
 */
public class BestScoreStrategy extends JointPlanStrategy {
	public BestScoreStrategy(final Controler controler) {
		this.planSelector = new BestPlanSelector();
	}
}

