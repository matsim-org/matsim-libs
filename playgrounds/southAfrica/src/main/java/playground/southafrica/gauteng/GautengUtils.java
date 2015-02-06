/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.southafrica.gauteng;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;

/**
 * @author nagel
 *
 */
final class GautengUtils {
	private GautengUtils() {} // do not instantiate; static methods onlay

	/**
	 * @param config
	 */
	static void assignSubpopulationStrategies(Config config) {
	
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8); 
	
		/* Set up the strategies for the different subpopulations. */
	
		{ /*
		 * Car: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta
					.toString());
			changeExpBetaStrategySettings.setSubpopulation("car");
			changeExpBetaStrategySettings.setWeight(0.7);
			config.strategy()
			.addStrategySettings(changeExpBetaStrategySettings);
	
			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator
					.toString());
			timeStrategySettings.setSubpopulation("car");
			timeStrategySettings.setWeight(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
	
			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setStrategyName(GautengControler_subpopulations.RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setWeight(0.15);
			reRouteWithId.setSubpopulation("car");
			config.strategy().addStrategySettings(reRouteWithId);
		}
	
		{ /*
		 * Commercial vehicles: ChangeExpBeta: 85%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta
					.toString());
			changeExpBetaStrategySettings.setSubpopulation("commercial");
			changeExpBetaStrategySettings.setWeight(0.80);
			config.strategy()
			.addStrategySettings(changeExpBetaStrategySettings);
	
			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setStrategyName(GautengControler_subpopulations.RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setWeight(0.20);
			reRouteWithId.setSubpopulation("commercial");
			config.strategy().addStrategySettings(reRouteWithId);
		}
	
		{ /*
		 * Bus: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta
					.toString());
			changeExpBetaStrategySettings.setSubpopulation("bus");
			changeExpBetaStrategySettings.setWeight(0.7);
			config.strategy()
			.addStrategySettings(changeExpBetaStrategySettings);
	
			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator
					.toString());
			timeStrategySettings.setSubpopulation("bus");
			timeStrategySettings.setWeight(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
	
			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setStrategyName(GautengControler_subpopulations.RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setWeight(0.15);
			reRouteWithId.setSubpopulation("bus");
			config.strategy().addStrategySettings(reRouteWithId);
		}
		{ /*
		 * Taxi: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta
					.toString());
			changeExpBetaStrategySettings.setSubpopulation("taxi");
			changeExpBetaStrategySettings.setWeight(0.7);
			config.strategy()
			.addStrategySettings(changeExpBetaStrategySettings);
	
			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator
					.toString());
			timeStrategySettings.setSubpopulation("taxi");
			timeStrategySettings.setWeight(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
	
			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setStrategyName(GautengControler_subpopulations.RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setWeight(0.15);
			reRouteWithId.setSubpopulation("taxi");
			config.strategy().addStrategySettings(reRouteWithId);
		}
		{ /*
		 * External traffic: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta
					.toString());
			changeExpBetaStrategySettings.setSubpopulation("ext");
			changeExpBetaStrategySettings.setWeight(0.7);
			config.strategy()
			.addStrategySettings(changeExpBetaStrategySettings);
	
			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
			.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator
					.toString());
			timeStrategySettings.setSubpopulation("ext");
			timeStrategySettings.setWeight(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
	
			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setStrategyName(GautengControler_subpopulations.RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setWeight(0.15);
			reRouteWithId.setSubpopulation("ext");
			config.strategy().addStrategySettings(reRouteWithId);
		}
	}

}
