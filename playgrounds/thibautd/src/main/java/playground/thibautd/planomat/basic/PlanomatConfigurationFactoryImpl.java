/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatConfigurationFactoryImpl.java
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
package playground.thibautd.planomat.basic;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.MutationOperator;

import playground.thibautd.planomat.api.PlanomatConfigurationFactory;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

/**
 * The default implementation of a PlanomatConfigurationFactory
 * @author thibautd
 */
public class PlanomatConfigurationFactoryImpl extends PlanomatConfigurationFactory {
	private static final Logger log =
		Logger.getLogger(PlanomatConfigurationFactoryImpl.class);

	private final Planomat2ConfigGroup planomatConfigGroup;

	public PlanomatConfigurationFactoryImpl(
			final Planomat2ConfigGroup configGroup) {
		planomatConfigGroup = configGroup;
	}

	@Override
	protected void setGeneticOperators(final Configuration config) throws Exception {
		config.addGeneticOperator(
				new CrossoverOperator(
					config,
					0.6d));
		config.addGeneticOperator(
				new MutationOperator(
					config,
					config.getPopulationSize()));
	}

	@Override
	protected void setNaturalSelectors(final Configuration config) throws Exception {
		BestChromosomesSelector bestChromsSelector =
			new BestChromosomesSelector(
				config,
				0.90d);
		bestChromsSelector.setDoubletteChromosomesAllowed( false );
		config.addNaturalSelector(bestChromsSelector, false);
	}

	@Override
	protected void setPopulationSize(final Configuration config) throws Exception {
		int populationSize = (int) Math.ceil(
				planomatConfigGroup.getPopIntercept() +
			planomatConfigGroup.getPopSlope() * config.getSampleChromosome().size());
		config.setPopulationSize( Math.max( 2 , populationSize ) );
	}
}

