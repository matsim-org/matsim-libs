/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerModule.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class JointPlanOptimizerModule extends AbstractMultithreadedModule {
	private static final Logger log = Logger.getLogger(JointPlanOptimizerModule.class);

	private final ScoringFunctionFactory scoringFunctionFactory;
	private final JointReplanningConfigGroup configGroup;

	public JointPlanOptimizerModule(
			GlobalConfigGroup globalConfigGroup,
			JointReplanningConfigGroup configGroup,
			ScoringFunctionFactory scoringFunctionFactory) {
		super(globalConfigGroup);
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.configGroup = configGroup;
	}

	//public JointPlanOptimizerModule(int numOfThreads) {
	//	super(numOfThreads);
	//	log.debug("JointPlanOptimizerModule constructed with numOfThreads");
	//}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		log.debug("JointPlanOptimizerModule.getPlanAlgoInstance called");
		return new JointPlanOptimizer(
					this.scoringFunctionFactory,
					this.configGroup);
	}
}

