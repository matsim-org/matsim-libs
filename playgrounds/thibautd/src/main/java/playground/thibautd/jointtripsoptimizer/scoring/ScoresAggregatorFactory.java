/* *********************************************************************** *
 * project: org.matsim.*
 * ScoresAggregatorFactory.java
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
package playground.thibautd.jointtripsoptimizer.scoring;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author thibautd
 */
public interface ScoresAggregatorFactory extends MatsimFactory {
	public ScoresAggregator createScoresAggregator(Collection<? extends Plan> individualPlans);
}

