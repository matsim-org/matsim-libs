/* *********************************************************************** *
 * project: org.matsim.*
 * DuplicatePersons.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PersonAlgorithm;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class DuplicatePersons {
	private static final Logger log =
		Logger.getLogger(DuplicatePersons.class);

	public static void main( final String[] args ) throws IOException {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-ip" , "--in-population" , null );
		parser.setDefaultValue( "-op" , "--out-population" , null );

		parser.setDefaultValue( "-ia" , "--in-attributes" , null );
		parser.setDefaultValue( "-oa" , "--out-attributes" , null );

		parser.setDefaultValue( "-r" , "--rate" , "10" );

		main( parser.parseArgs( args ) );
	}

	private static void main( final Args args ) throws IOException {
		final String inPopulation = args.getValue( "-ip" );
		final String outPopulation = args.getValue( "-op"  );

		final String inAttributes = args.getValue( "-ia"  );
		final String outAttributes = args.getValue( "-oa"  );
		
		final int rate = args.getIntegerValue( "-r" );

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming( true );

		final PopulationWriter writer =
			new PopulationWriter(
					scenario.getPopulation(),
					scenario.getNetwork() );
		writer.writeStartPlans( outPopulation );

		final Map<String, Set<String>> clones = new HashMap< >();
		pop.addAlgorithm( new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				final Set<String> cloneIds = new LinkedHashSet< >();
				final String id = person.getId().toString();
				clones.put( id , cloneIds );

				for ( int i=0; i < rate; i++ ) {
					final String currId = id +"-"+ i;
					cloneIds.add( currId );
					((PersonImpl) person).setId( Id.createPersonId( currId ) );
					writer.writePerson( person );
				}
			}
		});

		new MatsimPopulationReader( scenario ).parse( inPopulation );
		writer.writeEndPlans();

		if ( inAttributes != null ) filterAttributes( clones , inAttributes , outAttributes );
	}

	private static void filterAttributes(
			final Map<String, Set<String>> clones,
			final String inAttributes,
			final String outAttributes ) throws IOException {
		try ( final BufferedReader readLink = IOUtils.getBufferedReader( inAttributes );
				final BufferedWriter outLink = IOUtils.getBufferedWriter( outAttributes ) ) {
			outLink.write( readLink.readLine() );
			outLink.newLine();
			outLink.write( readLink.readLine() );
			outLink.newLine();

			outLink.write( readLink.readLine() );
			outLink.newLine();

			outLink.write( readLink.readLine() );
			outLink.newLine();


			final Counter counter = new Counter( "attributes for person # " );
			String s;
			for ( s = readLink.readLine();
					!s.contains( "</objectAttributes>" );
					s = readLink.readLine() ) {

				counter.incCounter();
				final AttributesReallocator atts = new AttributesReallocator( s );
				try {
					do { s = readLink.readLine(); } while ( atts.readLine( s ) );
				}
				catch ( Exception e ) {
					log.error( "error with line "+s );
					log.error( "object id "+atts.id );
					throw e;
				}

				final Set<String> currClones =
					clones.get( atts.id );

				for ( String clone : currClones ) {
					try {
						outLink.write( atts.withId( clone ) );
					}
					catch ( Exception e ) {
						log.error( "error with clone id "+clone );
						log.error( "object id "+atts.id );
						throw e;
					}
				}

			}
			counter.printCounter();
			outLink.write(s);
		}
	}

	private static class AttributesReallocator {
		private final String id;
		private final StringBuffer buffer = new StringBuffer();

		public AttributesReallocator( final String s ) {
			if ( !s.matches( ".*<object id=\".*\">" ) ) throw new IllegalArgumentException( s );
			this.id = s.substring(s.indexOf("=") + 2, s.indexOf(">") - 1);
		}

		public boolean readLine( final String s ) {
			if ( s.contains( "</objectAttributes>" ) ) return false;

			buffer.append( s+"\n" );
			return !s.endsWith("</object>");
		}

		public String withId( final String newId ) {
			final StringBuffer newbuff = new StringBuffer( "\t<object id=\""+newId+"\">\n" );
			newbuff.append( buffer );
			return newbuff.toString();
		}
	}

}

