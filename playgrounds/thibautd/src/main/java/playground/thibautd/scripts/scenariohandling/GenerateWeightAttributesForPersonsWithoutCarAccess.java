/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateWeightAttributesForPersonsWithoutCarAccess.java
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author thibautd
 */
public class GenerateWeightAttributesForPersonsWithoutCarAccess {
	public static void main(final String[] args) {
		final String attName = args[ 0 ];
		final double weight = Double.parseDouble( args[ 1 ] );
		final String populationFile = args[ 2 ];
		final String outObjectAttributesFile = args[ 3 ];

		final ObjectAttributes attrs = new ObjectAttributes();

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		((PopulationImpl) sc.getPopulation()).addAlgorithm(
			new PersonAlgorithm() {
				@Override
				public void run(final Person person) {
					final String carAvail = PersonImpl.getCarAvail(person);
					final String license = PersonImpl.getLicense(person);
					final boolean isCarAvail =
						!"no".equals( license ) &&
						!"never".equals( carAvail );
					if ( !isCarAvail ) {
						attrs.putAttribute(
							person.getId().toString(),
							attName,
							weight );
					}
				}
			});
		((PopulationImpl) sc.getPopulation()).setIsStreaming( true );
		new MatsimPopulationReader( sc ).readFile( populationFile );

		new ObjectAttributesXmlWriter( attrs ).writeFile( outObjectAttributesFile );
	}
}

