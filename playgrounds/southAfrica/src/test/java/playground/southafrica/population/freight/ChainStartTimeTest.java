/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.population.freight;

import org.junit.Assert;
import org.junit.Test;

public class ChainStartTimeTest {

	/**
	 * These tests are currently (Dec'13) only applicable to the 2009 Digicore
	 * data, and will/may fail once the static data is updated to newer 
	 * cumulative probabilities.
	 */
	@Test
	public void testGetStartTimeInSeconds() {
		/* Should be in the second hour, 01:00:00 - 02:00:00. */
		double d = 0.01;
		Assert.assertTrue("Should be after 01:00:00.", ChainStartTime.getStartTimeInSeconds(d) >= 1*3600);
		Assert.assertTrue("Should be before 02:00:00.", ChainStartTime.getStartTimeInSeconds(d) <= 2*3600);
		
		/* Should be in the sixteenth hour, 15:00:00 - 16:00:00. */
		d = 0.900;
		Assert.assertTrue("Should be after 14:00:00.", ChainStartTime.getStartTimeInSeconds(d) >= 14*3600);
		Assert.assertTrue("Should be before 15:00:00.", ChainStartTime.getStartTimeInSeconds(d) <= 15*3600);
	}

}
