/* *********************************************************************** *
 * project: org.matsim.*
 * IOTest.java
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
package playground.thibautd.analysis.joinabletripsidentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.MatsimTestUtils;

import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.TripRecord;

/**
 * @author thibautd
 */
public class IOTest {
	@Rule public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testConsistency() throws Exception {
		final String inFile = utils.getPackageInputDirectory() +"/trips.xml";
		final String outfile = utils.getOutputDirectory() + "/dumpedTrips.xml";

		JoinableTrips inputTrips = read( inFile );
		write( inputTrips , outfile );
		JoinableTrips outputTrips = read( outfile );

		compare( inputTrips , outputTrips );
	}

	private void compare(
			final JoinableTrips inputTrips,
			final JoinableTrips outputTrips) {
		assertEquals(
				"incompatible number of conditions",
				inputTrips.getConditions().size(),
				outputTrips.getConditions().size());

		assertTrue(
				"incompatible conditions",
				inputTrips.getConditions().containsAll( outputTrips.getConditions() ));

		Map<Id, TripRecord> inRecords = inputTrips.getTripRecords();
		Map<Id, TripRecord> outRecords = outputTrips.getTripRecords();

		assertEquals(
				"incompatible number of records",
				inRecords.size(),
				outRecords.size());

		for (Id id : inRecords.keySet()) {
			TripRecord inRecord = inRecords.get( id );
			TripRecord outRecord = outRecords.get( id );

			assertEquals(
					"incompatible agent Id",
					inRecord.getAgentId(),
					outRecord.getAgentId());

			assertEquals(
					"incompatible arrival time",
					inRecord.getArrivalTime(),
					outRecord.getArrivalTime(),
					MatsimTestUtils.EPSILON);

			assertEquals(
					"incompatible departure time",
					inRecord.getDepartureTime(),
					outRecord.getDepartureTime(),
					MatsimTestUtils.EPSILON);

			assertEquals(
					"incompatible destination type",
					inRecord.getDestinationActivityType(),
					outRecord.getDestinationActivityType());

			assertEquals(
					"incompatible destination link",
					inRecord.getDestinationLinkId(),
					outRecord.getDestinationLinkId());

			assertEquals(
					"incompatible leg number",
					inRecord.getLegNumber(),
					outRecord.getLegNumber());

			assertEquals(
					"incompatible mode",
					inRecord.getMode(),
					outRecord.getMode());
	
			assertEquals(
					"incompatible origin type",
					inRecord.getOriginActivityType(),
					outRecord.getOriginActivityType());

			assertEquals(
					"incompatible origin link",
					inRecord.getOriginLinkId(),
					outRecord.getOriginLinkId());

			assertEquals(
					"incompatible joinable trips count",
					inRecord.getJoinableTrips().size(),
					outRecord.getJoinableTrips().size());

			assertTrue(
					"incompatible joinable trips",
					inRecord.getJoinableTrips().containsAll( outRecord.getJoinableTrips() ));
		}
	}

	private static JoinableTrips read( final String inFile ) {
		JoinableTripsXmlReader reader = new JoinableTripsXmlReader();
		reader.parse(inFile);
		return reader.getJoinableTrips();
	}

	private static void write(
			final JoinableTrips trips,
			final String fileName ) {
		JoinableTripsXmlWriter writer = new JoinableTripsXmlWriter( trips );
		writer.write( fileName );
	}
}

