/* *********************************************************************** *
 * project: org.matsim.*
 * ConstraintsManager.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators;

import org.jgap.IChromosome;

import org.matsim.core.utils.collections.Tuple;

/**
 * Interface for defining classes responsible for handling constraints in genocop-based
 * genetic operators.
 *
 * @author thibautd
 */
public interface ConstraintsManager {
	/**
	 * Returns the interval of possible values for a particular double
	 * gene.
	 *
	 * @param chromosome the chromosome to analyse 
	 * @param i the index of the double gene in the chromosome
	 * @return the interval, in the form of the lower bound (first value of the tuple) and the
	 * width (second value of the tuple). That is, the interval is the set of the
	 * values equal to <tt>first + a * second</tt> with <tt>a</tt> lying between 0 and 1
	 *
	 * @throws IllegalArgumentException if the given index is out of range or do not
	 * corresponds to a double gene.
	 */
	public Tuple<Double , Double> getAllowedRange(IChromosome chromosome, int i);

	/**
	 * Gives access to the coefficient for the "simple" cross-over.
	 * This corresponds to the largest a such that the vector 
	 * (x_1, x_2, ..., x_(i-1), (1 - a)x_i + a*y_i, ..., (1 - a)x_n + a*y_n)
	 * respects the constraints. A value of one corresponds to a classical single
	 * point cross-over.
	 *
	 * @param firstMate the first parent chromosome: its genes values after the crossing
	 * point will be multiplied by the crossing coeff before the combinaison
	 * @param secondMate the second parent chromosome: its genes values after the crossing
	 * point will be multiplied by one minus the crossing coeff before the combinaison
	 * @param crossingPoint the index of the first gene which is part of the
	 * linear recombination.
	 * @return the crossing coef a
	 *
	 * @throws IllegalArgumentException if the given index is out of range.
	 */
	public double getSimpleCrossOverCoef(
			IChromosome firstMate,
			IChromosome secondMate,
			int crossingPoint);

	/**
	 * Gives access to the interval in which the coefficient for the "single" cross-over
	 * has to lie.
	 * A random number a lying in this interval would be such that the vectors 
	 * (x_1, x_2, ..., x_(i-1), (1 - a)x_i + a*y_i, x_(i+1), ..., x_n)
	 * and
	 * (y_1, y_2, ..., y_(i-1), (1 - a)y_i + a*x_i, y_(i+1), ..., y_n)
	 * respect the constraints.
	 *
	 * @param firstMate the first parent chromosome (x above)
	 * @param secondMate the second parent chromosome (y above)
	 * @param crossingPoint the index of the double value to cross
	 * @return the interval, in the form of the lower bound (first value of the tuple) and the
	 * width (second value of the tuple). That is, the interval is the set of the
	 * values equal to <tt>first + a * second</tt> with <tt>a</tt> lying between 0 and 1
	 *
	 * @throws IllegalArgumentException if the given index is out of range or do not
	 * corresponds to a double gene.
	 */
	public Tuple<Double,Double> getSingleCrossOverCoefInterval(
			IChromosome firstMate,
			IChromosome secondMate,
			int crossingPoint);

	/**
	 * Checks wether a chromosome respects the constraints.
	 * @param chromosome the chromosome to check
	 * @return true if the chromosome respects the constaints
	 */
	public boolean respectsConstraints( IChromosome chromosome );

	/**
	 * Randomises the double values  of the double genes, so that
	 * they respect the constraints. This method should use the
	 * random generator provided by the configuration object obtained
	 * from the chromosome.
	 * <br>
	 * This method is meant to be used at genotype initialisation.
	 *
	 * @param chromosome the chromosome to randomise
	 */
	public void randomiseChromosome( IChromosome chromosome );
}

