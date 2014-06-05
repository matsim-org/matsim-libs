/* *********************************************************************** *
 * project: org.matsim.*
 * FilterDefaultSubpopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.analysis.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author thibautd
 */
public class FilterDefaultSubpopulation {
	public static void main(final String[] args) {
		final String inPlansFile = args[ 0 ];
		final String inAttributes = args[ 1 ];
		final String outPlansFile = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		((PopulationImpl) sc.getPopulation()).setIsStreaming( true );

		final String attName = new PlansConfigGroup().getSubpopulationAttributeName();
		final PopulationWriter writer = new PopulationWriter( sc.getPopulation() , sc.getNetwork() );

		((PopulationImpl) sc.getPopulation()).addAlgorithm(
				new PersonAlgorithm() {
					@Override
					public void run(final Person person) {
						final String subpop = (String)
							sc.getPopulation().getPersonAttributes().getAttribute(
								person.getId().toString(),
								attName );
						if ( subpop == null ) writer.writePerson( person );
					}
				});

		new ObjectAttributesXmlReader( sc.getPopulation().getPersonAttributes() ).parse( inAttributes );

		writer.startStreaming( outPlansFile );
		new MatsimPopulationReader( sc ).readFile( inPlansFile );
		writer.closeStreaming();
	}
}

