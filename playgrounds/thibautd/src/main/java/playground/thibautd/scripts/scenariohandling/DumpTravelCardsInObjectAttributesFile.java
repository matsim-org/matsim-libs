/* *********************************************************************** *
 * project: org.matsim.*
 * DumpTravelCardsInObjectAttributesFile.java
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

import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.thibautd.utils.DesiresConverter;

/**
 * @author thibautd
 */
public class DumpTravelCardsInObjectAttributesFile {
	public static void main(final String[] args) {
		final String inPopulation = args[ 0 ];
		final String inFacilities = args[ 1 ];
		final String inNetwork = args[ 2 ];
		final String outAttributes = args[ 3 ];

		final Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		config.plans().setInputFile( inPopulation );
		config.network().setInputFile( inNetwork );
		config.facilities().setInputFile( inFacilities );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final ObjectAttributes attributes = new ObjectAttributes();

		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			final Set<String> cards = PersonImpl.getTravelcards(person);
			attributes.putAttribute( person.getId().toString() , "hasTravelcard" , !(cards == null || cards.isEmpty()) );
		}

		final ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter( attributes );
		writer.putAttributeConverter( Desires.class , new DesiresConverter() );
		writer.writeFile( outAttributes );
	}
}

