/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesConstructor.java
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;

/**
 * Class responsible for creating ready-to-export clique information.
 *
 * @author thibautd
 */
public interface CliquesConstructor {
	/**
	 * Groups individuals in cliques, and modifies their plans by including joint trips.
	 *
	 * @param individualPopulation the population containing the individuals to group.
	 * The plans of the individuals must be modified to the joint state.
	 * @param matings the joint trips determined by the mating algorithm.
	 *
	 * @return a map linking clique id to the list of individuals Ids.
	 */
	public Map<Id, List<Id>> processMatings(Population individualPopulation, List<Mating> matings);
}

