/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatFitnessFunction.java
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
package playground.thibautd.planomat.api;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;

/**
 * Defines a fitness function for the planomat optimisation procedure.
 * Contrary to the default in the Jgap library, it allows negative fitnesses.
 * <br>
 * In the Planomat v2 modular concept, this is the element responsible for
 * all the semantics, that is, the mapping genotype/phenotype. Implementation
 * of all other elements must be made to work with any implementation of
 * this class.
 *
 * @author thibautd
 */
public abstract class PlanomatFitnessFunction extends FitnessFunction {
	private static final long serialVersionUID = 1L;

	/**
	 * A replacement for FitnessFunction.NO_FITNESS_VALUE, which is -1.000...
	 * This default value signalling a fitness value that has not been computed yet doesn't work here because we operate also with negative fitness values
	 * which is not intended by JGAP.
	 */
	public static final double NO_FITNESS_VALUE = Double.NEGATIVE_INFINITY;

	/**
	 * Returns a Jgap chromosome, corresponding to the expected structure.
	 * This allows each implementation of a fitness function to define its
	 * prefered coding.
	 *
	 * @return a chromosome with default values for the genes.
	 * @throws InvalidConfigurationException if it is thrown during chromosome initialisation
	 */
	public abstract PlanomatChromosome getSampleChomosome()
		throws InvalidConfigurationException ;

	/**
	 * Modifies back the plan this fitness function represents (which
	 * should be stored at initialisation), according to a chromosome
	 * representation.
	 *
	 * @param chromosome the genotype of the plan
	 */
	public abstract void modifyBackPlan(IChromosome chromosome);

	// /////////////////////////////////////////////////////////////////////////
	// reimplementation of some methods to allow negative fitnesses
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public double getFitnessValue(final IChromosome a_subject) {
	    return evaluate(a_subject);
	}
}

