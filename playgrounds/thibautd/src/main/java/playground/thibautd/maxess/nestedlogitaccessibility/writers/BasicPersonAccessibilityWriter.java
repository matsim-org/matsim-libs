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
package playground.thibautd.maxess.nestedlogitaccessibility.writers;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class BasicPersonAccessibilityWriter implements MatsimWriter {
	private static final Logger log = Logger.getLogger( BasicPersonAccessibilityWriter.class );
	private final TObjectDoubleMap<Id<Person>> accessibilityPerPerson;
	private final Scenario scenario;

	public BasicPersonAccessibilityWriter(
			final Scenario scenario,
			final TObjectDoubleMap<Id<Person>> accessibilityPerPerson ) {
		this.accessibilityPerPerson = accessibilityPerPerson;
		this.scenario = scenario;
	}

	@Override
	public void write( final String filename ) {
		log.info( "Write accessibility per person to file "+filename );
		final Counter lineCounter = new Counter( "write accessibility for person # " );
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( filename ) ) {
			writer.write( "person_id\tfacility_id\tx\ty\taccessibility" );

			for ( TObjectDoubleIterator<Id<Person>> it = accessibilityPerPerson.iterator();
						it.hasNext(); ) {
				// need to go here and not in "for", because need to "advance" before first element...
				it.advance();
				lineCounter.incCounter();
				final Person person = scenario.getPopulation().getPersons().get( it.key() );
				final Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get( 0 );
				final Id<ActivityFacility> facilityId = firstActivity.getFacilityId();
				final Coord coord = firstActivity.getCoord() != null ?
							firstActivity.getCoord() :
							scenario.getActivityFacilities().getFacilities().get( facilityId ).getCoord();
				final double accessibility = it.value();

				writer.newLine();
				writer.write( person.getId()+"\t" );
				writer.write( facilityId+"\t" );
				writer.write( coord.getX()+"\t"+coord.getY()+"\t" );
				writer.write( accessibility+"" );
			}
			lineCounter.printCounter();
			log.info( "Write accessibility per person to file "+filename+" : DONE" );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}
