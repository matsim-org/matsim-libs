/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;


public class CadytsRunner {

	public static void main(String[] args) {
		final Controler controler = new Controler(args);
		
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		controler.addControlerListener(cContext);
		
		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setStrategyName("ccc") ;
		stratSets.setWeight(1.0) ;
		controler.getConfig().strategy().addStrategySettings(stratSets);
		
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy get() {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(controler.getScenario(),cContext);
				return new PlanStrategyImpl(planSelector);
			}
		});
		
		controler.run();
	}
}
