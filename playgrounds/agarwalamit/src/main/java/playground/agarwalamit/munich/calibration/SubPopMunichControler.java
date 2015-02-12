/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.calibration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit
 */

public class SubPopMunichControler {
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(sc);
		

		
		controler.addPlanStrategyFactory("subtourModeChoice_URBAN", new PlanStrategyFactory() {
			String [] availableModes = {"car","pt_URBAN"};
			String [] chainBasedModes = {"car","bike"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new SubtourModeChoice(scenario.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});
		
		controler.addPlanStrategyFactory("subtourModeChoice_COMMUTER", new PlanStrategyFactory() {
			String [] availableModes = {"car","pt_COMMUTER"};
			String [] chainBasedModes = {"car","bike"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new SubtourModeChoice(scenario.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});
		
		controler.addPlanStrategyFactory("subtourModeChoice_REV_COMMUTER", new PlanStrategyFactory() {
			String [] availableModes = {"car","pt_REV_COMMUTER"};
			String [] chainBasedModes = {"car","bike"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new SubtourModeChoice(scenario.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});
		
		// freight.. change of mode is not possible
	}

}
