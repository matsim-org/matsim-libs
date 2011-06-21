/* *********************************************************************** *
 * project: org.matsim.*
 * TabuMonitor.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.selectors;

import org.jgap.IChromosome;
import org.jgap.Population;

/**
 * monitors the population and "corrects" tabu chromosomes.
 *
 * @deprecated this method is computationnally inefficient, and premature
 * convergence can be avoided much more efficiently.
 * @author thibautd
 */
@Deprecated
public interface TabuMonitor {
	/**
	 * To call at each generation to update tabu list.
	 */
	public void updateTabu(final Population population);

	/**
	 * checks a chromosome and mutates it until it is not tabu (if it was)
	 * @param chromosome a chromosome to (enventually) correct.
	 */
	public void correctTabu(final IChromosome chromosome);
}

