/* *********************************************************************** *
 * project: org.matsim.*
 * CreateGroupsAndPrintSizeSaturday.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.MZ2010;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author thibautd
 */
public class CreateGroupsAndPrintSizeSaturday {
	public static void main(final String[] args) {
		final String popFile = args[ 0 ];
		final String attsFile = args[ 1 ];

		Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( sc ).parse( popFile );
		ObjectAttributes atts = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader( atts );
		reader.putAttributeConverter( Gender.class , new SimplifyObjectAttributes.GenderConverter() );
		reader.parse( attsFile );

		MzGroups groups = new MzGroups();
		Counter count = new Counter( "adding person # " );
		for (Person p : sc.getPopulation().getPersons().values()) {
			if ( atts.getAttribute( p.getId().toString() , SimplifyObjectAttributes.DOW ).equals( 6 ) ) {
				count.incCounter();
				if ( !groups.add( atts , p ) ) throw new RuntimeException();
			}
		}
		count.printCounter();

		groups.printInfo();
	}
}

