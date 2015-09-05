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
package playground.thibautd.socnetsimusages.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import playground.ivt.utils.Desires;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.thibautd.utils.DesiresConverter;

import java.util.Map;

/**
 * @author thibautd
 */
public final class ZurichScenarioUtils {
	private final static Logger log = Logger.getLogger( ZurichScenarioUtils.class );

	private ZurichScenarioUtils() {}

	private static final String UNKOWN_TRAVEL_CARD = "unknown";
	public static void enrichScenario( final Scenario scenario ) {
		final Config config = scenario.getConfig();
		if ( config.plans().getInputPersonAttributeFile() != null ) {
			log.info( "re-reading attributes, this time using a converter for Desires." );
			final ObjectAttributesXmlReader reader =
				new ObjectAttributesXmlReader(
						scenario.getPopulation().getPersonAttributes());
			reader.putAttributeConverter( Desires.class , new DesiresConverter() );
			reader.parse(
				config.plans().getInputPersonAttributeFile() );

			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				// put desires (if any) in persons for backward compatibility
				final Desires desires = (Desires)
					scenario.getPopulation().getPersonAttributes().getAttribute(
							person.getId().toString(),
							"desires" );
				if ( desires != null ) {
					((PersonImpl) person).createDesires( desires.getDesc() );
					for ( Map.Entry<String, Double> entry : desires.getActivityDurations().entrySet() ) {
						((PersonImpl) person).getDesires().putActivityDuration(
							entry.getKey(),
							entry.getValue() );
					}
				}

				// travel card
				final Boolean hasCard = (Boolean)
					scenario.getPopulation().getPersonAttributes().getAttribute(
							person.getId().toString(),
							"hasTravelcard" );
				if ( hasCard != null && hasCard ) {
					((PersonImpl) person).addTravelcard( UNKOWN_TRAVEL_CARD );
				}
			}
		}
	}
}
