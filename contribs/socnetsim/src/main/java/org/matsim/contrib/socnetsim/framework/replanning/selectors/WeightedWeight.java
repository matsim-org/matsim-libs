/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedWeight.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class WeightedWeight implements WeightCalculator {
	private final WeightCalculator delegate;

	private final String weightAttribute;
	private final Population population;

	public WeightedWeight(
			final WeightCalculator delegate,
			final String weightAttribute,
			final Population population ) {
		this.delegate = delegate;
		this.weightAttribute = weightAttribute;
		this.population = population;
	}

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup) {
		final Double weight = (Double)
//			personAttributes.getAttribute(
//				indivPlan.getPerson().getId().toString(),
//				weightAttribute );
							PopulationUtils.getPersonAttribute( indivPlan.getPerson(), weightAttribute, population ) ;
		return (weight == null ? 1 : weight.doubleValue()) * delegate.getWeight( indivPlan , replanningGroup );
	}
}

