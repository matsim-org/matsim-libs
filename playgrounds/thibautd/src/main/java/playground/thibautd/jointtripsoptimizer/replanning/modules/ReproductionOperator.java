/* *********************************************************************** *
 * project: org.matsim.*
 * ReproductionOperator.java
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
* The reproduction operator makes a copy of each Chromosome in the
* population and adds it to the list of candidate chromosomes. This
* essentially guarantees that each Chromosome in the current population
* remains a candidate for selection for the next population.
*/
public class ReproductionOperator implements GeneticOperator
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
* The operate method will be invoked on each of the genetic operators
* referenced by the current Configuration object during the evolution
* phase. Operators are given an opportunity to run in the order that
* they are added to the Configuration. Implementations of this method
* may reference the population of Chromosomes as it was at the beginning
* of the evolutionary phase or the candidate Chromosomes, which are the
* results of prior genetic operators. In either case, only Chromosomes
* added to the list of candidate chromosomes will be considered for
* natural selection. Implementations should never modify the original
* population.
*
* @param a_population the population of chromosomes from the current
* evolution prior to exposure to any genetic operators
* @param a_candidateChromosomes the pool of chromosomes that are candidates
* for the next evolved population. Any chromosomes that are modified by this
* genetic operator that should be considered for natural selection should be
* added to the candidate chromosomes
*/
    public void operate( final Population a_population,
                         final List a_candidateChromosomes )
    {
        // Just loop over the chromosomes in the population, make a copy of
        // each one, and then add that copy to the candidate chromosomes
        // pool so that it'll be considered for natural selection during the
        // next phase of evolution.
        // -----------------------------------------------------------------
        int len = a_population.size();
        for ( int i = 0; i < len; i++ )
        {
            a_candidateChromosomes.add( a_population.getChromosome(i).clone() );
        }
    }
}
