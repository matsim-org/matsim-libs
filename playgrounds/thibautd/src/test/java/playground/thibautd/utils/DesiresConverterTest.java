/* *********************************************************************** *
 * project: org.matsim.*
 * DesiresConverterTest.java
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
package playground.thibautd.utils;

import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.population.Desires;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class DesiresConverterTest {

	@Test
	public void testConvertAndGetBack() {
		Logger.getLogger( DesiresConverter.class ).setLevel( Level.TRACE );
		final DesiresConverter converter = new DesiresConverter();

		final Desires desires = new Desires( null );

		desires.putActivityDuration( "some type" , 100 );
		desires.putActivityDuration( "some other type" , 1000 );

		final String repr = converter.convertToString( desires );
		final Desires reconverted = converter.convert( repr );

		Assert.assertEquals(
				"unexpected number of durations",
				desires.getActivityDurations().size(),
				reconverted.getActivityDurations().size() );

		for ( Map.Entry<String, Double> entry : desires.getActivityDurations().entrySet() ) {
			final String type = entry.getKey();
			Assert.assertEquals(
					"unexpected duration for type "+type,
					entry.getValue(),
					reconverted.getActivityDuration( type ),
					MatsimTestUtils.EPSILON);
		}
	}
}

