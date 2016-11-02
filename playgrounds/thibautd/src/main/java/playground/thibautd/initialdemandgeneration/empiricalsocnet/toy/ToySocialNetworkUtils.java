/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.toy;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

/**
 * @author thibautd
 */
public class ToySocialNetworkUtils {
	private ToySocialNetworkUtils() {}

	public static Scenario generateRandomScenario(
			final Random random,
			final Config config ) {
		final ToySocialNetworkConfigGroup configGroup = (ToySocialNetworkConfigGroup) config.getModule( ToySocialNetworkConfigGroup.GROUP_NAME );
		final Scenario scenario = ScenarioUtils.createScenario( config );
		final Population population = scenario.getPopulation();

		for ( int i=0; i < configGroup.getPopulationSize(); i++ ) {
			final Person person = population.getFactory().createPerson( Id.createPersonId( i ) );
			population.addPerson( person );
			person.getCustomAttributes().put(
					"coord" ,
					new Coord(
							random.nextDouble() * configGroup.getWidth_m(),
							random.nextDouble() * configGroup.getWidth_m() ) );
		}

		return scenario;
	}
}
