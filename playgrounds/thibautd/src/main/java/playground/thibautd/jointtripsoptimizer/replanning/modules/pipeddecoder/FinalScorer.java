/* *********************************************************************** *
 * project: org.matsim.*
 * FinalScorer.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder;

import org.jgap.IChromosome;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Interface meant at building "on the fly" scorer in a modular way.
 * This class takes a partially decoded plan and a chromosome, and computes
 * the fitness based on the yet undecoded dimensions (eg durations).
 *
 * @author thibautd
 */
public interface FinalScorer {
	/**
	 * @param chromosome the representation of the plan to score
	 * @param inputPlan a plan in wich all dimensions unhandled by this class
	 * are corretly set.
	 * @return the score of the plan coded by chromosome
	 */
	public double score(IChromosome chromosome, JointPlan inputPlan);
}

