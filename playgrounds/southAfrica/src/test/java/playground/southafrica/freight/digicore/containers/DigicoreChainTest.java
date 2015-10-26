/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreActivityTest.java
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

package playground.southafrica.freight.digicore.containers;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

public class DigicoreChainTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	public void testConstructor(){
		//TODO Finish
	}
	
	
	@Test
	public void testGetChainStartDay(){
		/* Thursday, 4 October 2012 */
		GregorianCalendar gc1a = new GregorianCalendar(2012, Calendar.OCTOBER, 4, 0, 0, 0); gc1a.setTimeZone(TimeZone.getTimeZone("GMT+2"));    // 00:00:00
		GregorianCalendar gc1b = new GregorianCalendar(2012, Calendar.OCTOBER, 4, 8, 0, 0); gc1b.setTimeZone(TimeZone.getTimeZone("GMT+2"));    // 08:00:00
		GregorianCalendar gc2a = new GregorianCalendar(2012, Calendar.OCTOBER, 4, 17, 0, 0); gc2a.setTimeZone(TimeZone.getTimeZone("GMT+2"));   // 17:00:00
		GregorianCalendar gc2b = new GregorianCalendar(2012, Calendar.OCTOBER, 4, 23, 59, 59); gc2b.setTimeZone(TimeZone.getTimeZone("GMT+2")); // 23:59:59
		
		/* Create chain. */
		DigicoreChain dc = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("test1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setStartTime((double)gc1a.getTimeInMillis() / (double)1000);
		da1.setEndTime((double)gc1b.getTimeInMillis() / (double)1000);
		da1.setType("major");
		dc.add(da1);
		
		DigicoreActivity da2 = new DigicoreActivity("test2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setStartTime((double)gc2a.getTimeInMillis() / (double)1000);
		da2.setEndTime((double)gc2b.getTimeInMillis() / (double)1000);
		da2.setType("major");
		dc.add(da2);
		
		Assert.assertEquals("Wrong start day.", Calendar.THURSDAY, dc.getChainStartDay());
	}
	
	
	public void testContainsFacility(){
		
		/* Create chain. */
		DigicoreChain dc = new DigicoreChain();
		DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setFacilityId(Id.create("f2", ActivityFacility.class));
		da3.setFacilityId(Id.create("f3", ActivityFacility.class));
		dc.add(da1);
		dc.add(da2);
		dc.add(da3);
		
		Assert.assertFalse("Should not have found facility.", dc.containsFacility(Id.create("f1", ActivityFacility.class)));
		Assert.assertTrue("Should have found facility.", dc.containsFacility(Id.create("f2", ActivityFacility.class)));
		Assert.assertTrue("Should have found facility.", dc.containsFacility(Id.create("f3", ActivityFacility.class)));
		Assert.assertTrue("Should have found NULL facility.", dc.containsFacility(null));

	}

}

