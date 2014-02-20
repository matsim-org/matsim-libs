/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveLinksAndRoutesFromPop.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonRemoveLinkAndRoute;

/**
 * @author thibautd
 */
public class RemoveLinksAndRoutesFromPop {
	public static void main(final String[] args) {
		final String inPop = args[ 0 ];
		final String outPop = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( sc ).parse( inPop );

		final PersonAlgorithm algo = new PersonRemoveLinkAndRoute();
		for ( Person p : sc.getPopulation().getPersons().values() ) {
			algo.run( p );
		}

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outPop );
	}
}

