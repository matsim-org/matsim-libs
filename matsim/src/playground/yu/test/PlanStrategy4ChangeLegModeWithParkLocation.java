/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStrategy4ChangeLegModeWithParkLocation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.test;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author yu
 * 
 */
public class PlanStrategy4ChangeLegModeWithParkLocation extends PlanStrategy {
	public PlanStrategy4ChangeLegModeWithParkLocation(PlanSelector planSelector) {
		super(planSelector);
		this.addStrategyModule(new ChangeLegModeWithParkLocation(Gbl
				.getConfig()));
	}

	// public PlanStrategy4ChangeLegModeWithParkLocation(BasicScenario bs) {
	// this(new ExpBetaPlanChanger());
	// this.addStrategyModule(new ChangeLegModeWithParkLocation(Gbl
	// .getConfig()));
	// }
}
