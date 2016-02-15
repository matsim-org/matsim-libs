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
package playground.ivt.maxess.nestedlogitaccessibility.writers;

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
import playground.ivt.maxess.nestedlogitaccessibility.framework.AccessibilityComputationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class BasicPersonAccessibilityWriter implements MatsimWriter {
	private static final Logger log = Logger.getLogger( BasicPersonAccessibilityWriter.class );
	private final AccessibilityComputationResult accessibilityPerPerson;
	private final Scenario scenario;

	private final List<ColumnCalculator> additionalColumns = new ArrayList<>();

	public BasicPersonAccessibilityWriter(
			final Scenario scenario,
			final AccessibilityComputationResult accessibilityPerPerson,
			final ColumnCalculator... columns ) {
		this.accessibilityPerPerson = accessibilityPerPerson;
		this.scenario = scenario;
		for ( ColumnCalculator c : columns ) addColumnCalculator( c );
	}

	public void addColumnCalculator( final ColumnCalculator c ) {
		this.additionalColumns.add( c );
	}

	@Override
	public void write( final String filename ) {
		log.info( "Write accessibility per person to file "+filename );
		final Counter lineCounter = new Counter( "write accessibility for person # " );
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( filename ) ) {
			writer.write( "person_id\tfacility_id\tx\ty" );
			for ( String name : accessibilityPerPerson.getTypes() ) {
				writer.write( "\taccessibility_"+name );
			}
			for ( ColumnCalculator c : additionalColumns ) {
				writer.write( "\t"+c.getColumnName() );
			}

			for ( Map.Entry<Id<Person>,AccessibilityComputationResult.PersonAccessibilityComputationResult> e : accessibilityPerPerson.getResultsPerPerson().entrySet() ) {
				lineCounter.incCounter();
				final Person person = scenario.getPopulation().getPersons().get( e.getKey() );
				final Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get( 0 );
				final Id<ActivityFacility> facilityId = firstActivity.getFacilityId();
				final Coord coord = firstActivity.getCoord() != null ?
							firstActivity.getCoord() :
							scenario.getActivityFacilities().getFacilities().get( facilityId ).getCoord();
				final Map<String, Double> accessibilities = e.getValue().getAccessibilities();

				writer.newLine();
				writer.write( person.getId()+"\t" );
				writer.write( facilityId+"\t" );
				writer.write( coord.getX() + "\t" + coord.getY() );
				for ( String name : accessibilityPerPerson.getTypes() ) {
					final Double a = accessibilities.get( name );
					writer.write( "\t"+( a != null ? a : "NA" ) );
				}
				for ( ColumnCalculator c : additionalColumns ) {
					writer.write( "\t"+c.computeValue( e.getValue() ) );
				}
			}
			lineCounter.printCounter();
			log.info( "Write accessibility per person to file "+filename+" : DONE" );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	public interface ColumnCalculator {
		String getColumnName();
		double computeValue( AccessibilityComputationResult.PersonAccessibilityComputationResult personResults );
	}
}
