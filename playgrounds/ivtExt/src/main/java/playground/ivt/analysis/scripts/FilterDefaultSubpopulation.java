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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

/**
 * @author thibautd
 */
public class FilterDefaultSubpopulation {
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-p" , "--in-plans-file" , null );
		parser.setDefaultValue( "-a" , "--in-atts-file" , null );
		parser.setDefaultValue( "-o" , "--out-plans-file" , null );

		final Args parsed = parser.parseArgs( args );
		final String inPlansFile = parsed.getValue( "-p" );
		final String inAttributes = parsed.getValue( "-a" );
		final String outPlansFile = parsed.getValue( "-o" );

		final MutableScenario sc = ScenarioUtils.createMutableScenario( ConfigUtils.createConfig() );
//		final Population population = (Population) sc.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( sc ) ;
		StreamingDeprecated.setIsStreaming(reader, true);

		final String attName = new PlansConfigGroup().getSubpopulationAttributeName();
		final StreamingPopulationWriter writer = new StreamingPopulationWriter( );

		reader.addAlgorithm(new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				final String subpop = (String)
					sc.getPopulation().getPersonAttributes().getAttribute(
						person.getId().toString(),
						attName );
				if ( subpop == null ) writer.writePerson( person );
			}
		});

		new ObjectAttributesXmlReader( sc.getPopulation().getPersonAttributes() ).readFile( inAttributes );

		writer.startStreaming( outPlansFile );
		new PopulationReader( sc ).readFile( inPlansFile );
		writer.closeStreaming();
	}
}

