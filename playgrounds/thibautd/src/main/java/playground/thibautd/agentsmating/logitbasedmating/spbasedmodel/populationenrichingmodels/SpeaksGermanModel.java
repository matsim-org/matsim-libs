/* *********************************************************************** *
 * project: org.matsim.*
 * SpeaksGermanModel.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.populationenrichingmodels;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.ReducedModelParametersConfigGroup;

/**
 * Says if a person speaks german. It is a map rather than a model (the "model" is used
 * to create the file from which language info is extracted).
 * @author thibautd
 */
public class SpeaksGermanModel {
	private static final Logger log =
		Logger.getLogger(SpeaksGermanModel.class);

	private static final String GERMAN = "g";
	private final Map<Id, Boolean> map = new HashMap<Id, Boolean>();

	/**
	 * @param configGroup the config group, used to get the id2lang file
	 */
	public SpeaksGermanModel(
			final ReducedModelParametersConfigGroup configGroup) {
		this( configGroup.getLanguageFile() );
	}

	/**
	 * @param file the name of the file
	 */
	public SpeaksGermanModel( final String file ) {
		log.info( "reading id2language file: "+file+"..." );
		BufferedReader reader = IOUtils.getBufferedReader( file );

		try {
			Counter count = new Counter( "language info: reading line # " );
			for ( String line = reader.readLine();
					line != null;
					line = reader.readLine() ) {
				count.incCounter();
				String[] array = line.split( "\t" );
				map.put(
						new IdImpl( array[ 0 ].trim() ) , 
						GERMAN.equals( array[ 1 ] ) );
			}
			count.printCounter();

			reader.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		log.info( "reading id2language file: "+file+"... DONE" );
	}

	/**
	 * @param person the person for which to retrieve language information
	 * @return true if the person's language matches "german"
	 * @throws UnknownPersonException if the person is unknown. several ways to handle this may be valid.
	 */
	public boolean speaksGerman(final Person person) throws UnknownPersonException {
		return speaksGerman( person.getId() );
	}

	/**
	 * @param id the id of the person
	 * @return true if the person's language matches "german"
	 * @throws UnknownPersonException if the person is unknown. several ways to handle this may be valid.
	 */
	public boolean speaksGerman( final Id id ) throws UnknownPersonException {
		Boolean b = map.get( id );

		if (b == null) {
			throw new UnknownPersonException( "no language value for id "+id ); 
		}
		return b;
	}

	// /////////////////////////////////////////////////////////////////////////
	// for tests
	// /////////////////////////////////////////////////////////////////////////
	int size() {
		return map.size();
	}

	// /////////////////////////////////////////////////////////////////////////
	// Informative Exception
	// /////////////////////////////////////////////////////////////////////////
	public static class UnknownPersonException extends Exception {
		private UnknownPersonException(final String message) {
			super( message );
		}
	}
}

