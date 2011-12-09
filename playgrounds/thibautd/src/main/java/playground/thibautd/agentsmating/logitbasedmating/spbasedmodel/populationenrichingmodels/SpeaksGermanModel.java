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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

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

	public SpeaksGermanModel(
			final ReducedModelParametersConfigGroup configGroup) {

		String file = configGroup.getLanguageFile();
		log.info( "reading id2language file: "+file+"..." );
		BufferedReader reader = IOUtils.getBufferedReader( file );

		try {
			for ( String line = reader.readLine();
					line != null;
					line = reader.readLine() ) {
				String[] array = line.split( "\t" );
				map.put(
						new IdImpl( array[ 0 ].trim() ) , 
						GERMAN.equals( array[ 1 ] ) );
			}
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		log.info( "reading id2language file: "+file+"... DONE" );
	}

	/**
	 * @throws IllegalArgumentException if the person is unknown
	 */
	public boolean speaksGerman(final Person person) {
		Boolean b = map.get( person.getId() );

		if (b == null) {
			throw new IllegalArgumentException( "no language value for id"+person.getId() ); 
		}
		return b;
	}
}

