/* *********************************************************************** *
 * project: org.matsim.*
 * CountTripsPerTypeOfOriginAndDestination.java
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
package org.matsim.contrib.socnetsim.usage.analysis.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.JointMainModeIdentifier;

/**
 * @author thibautd
 */
public class CountTripsPerTypeOfOriginAndDestination {
	private static final MainModeIdentifier MODE_IDENTIFIER = 
		new MainModeIdentifier() {
			final MainModeIdentifier delegate = new JointMainModeIdentifier( new MainModeIdentifierImpl() );

			@Override
			public String identifyMainMode(final List<? extends PlanElement> tripElements) {
				if ( tripElements.size() == 1 &&
						((Leg) tripElements.get( 0 )).getMode().equals( TransportMode.transit_walk ) ) {
					return TransportMode.walk;
				}
				return delegate.identifyMainMode( tripElements );
			}
		};
	private static final StageActivityTypes STAGES =
		new StageActivityTypesImpl(
				Arrays.asList(
					PtConstants.TRANSIT_ACTIVITY_TYPE,
					JointActingTypes.INTERACTION ) );


	public static void main(final String[] args) throws IOException {
		final String inPlansFile = args[ 0 ];
		final String outFile = args[ 1 ];

		final Population population = readPopulation( inPlansFile );
		final Iterable<Record> counts = countTrips( population );
		writeCounts( counts , outFile );
	}

	private static void writeCounts(
			final Iterable<Record> counts,
			final String outFile) throws IOException {
		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "origType\tdestType\tmode\tcount" );
		for ( Record r : counts ) {
			writer.newLine();
			writer.write( r.originType+"\t"+
					r.destinationType+"\t"+
					r.mode+"\t"+
					r.count );
		}
		writer.close();
	}

	private static Iterable<Record> countTrips(final Population population) {
		final QueryableSet records = new QueryableSet();
		for ( Person person : population.getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			for ( Trip trip : TripStructureUtils.getTrips( plan , STAGES ) ) {
				final Record r = records.getOrAddIfNotThere(
						new Record(
							trip.getOriginActivity().getType(),
							trip.getDestinationActivity().getType(),
							MODE_IDENTIFIER.identifyMainMode( trip.getTripElements() ) ) );
				r.count++;
			}
		}
		return records;
	}

	private static Population readPopulation(final String inPlansFile) {
		final Scenario s = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( s ).readFile( inPlansFile );
		return s.getPopulation();
	}

	private static class QueryableSet implements Iterable<Record> {
		private final HashMap<Record, Record> map = new HashMap<Record, Record>();

		public Record getOrAddIfNotThere( final Record record ) {
			final Record alreadyThere = map.get( record );
			if ( alreadyThere != null ) return alreadyThere;

			map.put( record , record );
			return record;
		}

		@Override
		public Iterator<Record> iterator() {
			return map.keySet().iterator();
		}
	}

	private static class Record {
		final String originType, destinationType, mode;
		int count = 0;

		public Record(final String ot, final String dt, final String mode) {
			this.originType = ot;
			this.destinationType = dt;
			this.mode = mode;
		}

		@Override
		public boolean equals(final Object o) {
			return o instanceof Record &&
				originType.equals( ((Record) o).originType ) &&
				destinationType.equals( ((Record) o).destinationType ) &&
				mode.equals( ((Record) o).mode );
		}

		@Override
		public int hashCode() {
			return originType.hashCode() + destinationType.hashCode() + mode.hashCode();
		}
	}
}

