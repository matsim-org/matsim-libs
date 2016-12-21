/* *********************************************************************** *
 * project: org.matsim.*
 * SamplePersons.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author thibautd
 */
public class SamplePersons {
	public static void main( final String[] args ) throws IOException {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-ip" , "--in-population" , null );
		parser.setDefaultValue( "-op" , "--out-population" , null );

		parser.setDefaultValue( "-ia" , "--in-attributes" , null );
		parser.setDefaultValue( "-oa" , "--out-attributes" , null );

		parser.setDefaultValue( "-r" , "--rate" , "0.1" );

		main( parser.parseArgs( args ) );
	}

	private static void main( final Args args ) throws IOException {
		final String inPopulation = args.getValue( "-ip" );
		final String outPopulation = args.getValue( "-op"  );

		final String inAttributes = args.getValue( "-ia"  );
		final String outAttributes = args.getValue( "-oa"  );
		
		final double rate = args.getDoubleValue( "-r" );

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Population pop = (Population) scenario.getPopulation();
		StreamingDeprecated.setIsStreaming(pop, true);

		final StreamingPopulationWriter writer =
			new StreamingPopulationWriter( );
		writer.writeStartPlans( outPopulation );

		final Random random = new Random( 1234 );
		final Set<Id<Person>> kept = new HashSet< >();
		StreamingDeprecated.addAlgorithm(pop, new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				if ( random.nextDouble() > rate ) return;
				kept.add( person.getId() );
				writer.writePerson( person );
			}
		});

		new PopulationReader( scenario ).readFile( inPopulation );
		writer.writeEndPlans();

		if ( inAttributes != null ) filterAttributes( kept, inAttributes, outAttributes );
	}

	private static void filterAttributes(
			final Set<Id<Person>> kept,
			final String inAttributes,
			 final String outAttributes ) throws IOException {
		try (
				final BufferedReader readLink = IOUtils.getBufferedReader( inAttributes );
				final BufferedWriter outLink = IOUtils.getBufferedWriter( outAttributes ) ) {
			outLink.write(readLink.readLine());
			outLink.newLine();
			outLink.write(readLink.readLine());
			outLink.newLine();

			outLink.write(readLink.readLine());
			outLink.newLine();

			outLink.write(readLink.readLine());
			outLink.newLine();

			String s = readLink.readLine();
					
			final Counter counter = new Counter( "attributes for person # " );
			while(!s.contains("</objectAttributes>")) {
				
				if ( kept.contains( Id.create(s.substring(s.indexOf("=") + 2, s.indexOf(">") - 1), Person.class) )){
					counter.incCounter();
					outLink.write(s);
					outLink.newLine();
					s = readLink.readLine();
					while (!s.contains("</object>")) {
						
						outLink.write(s);
						outLink.newLine();

						s = readLink.readLine();
						
					}
					outLink.write(s);
					outLink.newLine();
					s = readLink.readLine();
				}
				else {
					s = readLink.readLine();
				}
				
			}
			counter.printCounter();
			outLink.write(s);
		}
	}

}

