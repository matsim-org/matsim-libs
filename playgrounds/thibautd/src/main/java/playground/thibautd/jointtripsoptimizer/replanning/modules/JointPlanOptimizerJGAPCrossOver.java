/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPCrossOver.java
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

import java.util.List;

import org.jgap.GeneticOperator;
import org.jgap.Population;

/**
 * Class making a uniform cross-over between alternatives.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPCrossOver implements GeneticOperator {

	private static final long serialVersionUID = 1L;

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {

	}
}

