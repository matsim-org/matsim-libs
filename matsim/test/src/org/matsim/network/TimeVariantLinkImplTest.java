/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantLinkImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.network;

import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;

/**
 * @author mrieser
 * @author laemmel
 */
public class TimeVariantLinkImplTest extends MatsimTestCase {

	/** Tests the method {@link TimeVariantLinkImpl#getFreespeedTravelTime(double)}.	 */
	public void testGetFreespeedTravelTime(){
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.createNode("1", "0", "0", null);
		network.createNode("2", "0", "1000", null);
		network.createNode("3", "1000", "2000", null);
		network.createNode("4", "2000", "2000", null);
		network.createNode("5", "1000", "0", null);
		final Link link1 = network.createLink("1", "1", "2", "1000", "1.667", "3600", "1", null, null);
//		final Link link2 = network.createLink("2", "2", "3", "1500", "1.667", "3600", "1", null, null);
		final Link link3 = network.createLink("3", "3", "4", "1000", "1.667", "3600", "1", null, null);
//		final Link link4 = network.createLink("4", "4", "5", "2800", "1.667", "3600", "1", null, null);

		final double [] queryDates = {org.matsim.utils.misc.Time.UNDEFINED_TIME, 0., 1., 2., 3., 4.};

		// link1 change event absolute, undef. endtime
		final double [] responsesLink1 = {1.667, 1.667, 10., 10., 10., 10.};
		NetworkChangeEvent event = new NetworkChangeEvent(1);
		event.addLink(link1);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,10.));
		((TimeVariantLinkImpl)link1).applyEvent(event);

		// link3 change event factor, undef. endtime
		final double [] responsesLink3 = {1.667, 1.667, 10.002, 10.002, 10.002, 10.002};
		event = new NetworkChangeEvent(1);
		event.addLink(link3);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,6.));
		((TimeVariantLinkImpl)link3).applyEvent(event);

		for (int i = 0; i < queryDates.length; i++) {
			assertEquals(responsesLink1[i], link1.getFreespeed(queryDates[i]), EPSILON);
			assertEquals(responsesLink3[i], link3.getFreespeed(queryDates[i]), EPSILON);
		}
	}

	/**
	 * Tests whether an absolute change in the freespeed really can be seen in the link's travel time
	 */
	public void testFreespeedChangeAbsolute() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);

		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink("1", "1", "2", "100", "10", "3600", "1", null, null);

		// test base values
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON);

		// add an absolute change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		link.applyEvent(change);

		// do the tests
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON); // at undefined time, return base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), EPSILON);  // one second before change, still base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 0.1), EPSILON);  // just before change, still base value
		assertEquals(20.0, link.getFreespeed(7*3600.0), EPSILON); // just on time of change, new value
		assertEquals(20.0, link.getFreespeed(8*3600.0), EPSILON); // some time later, still new value

		// test derived values
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON); // and now the same tests for the travel time
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 1.0), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 0.1), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(7*3600.0), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(8*3600.0), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(24*3600.0), EPSILON); // also test if it "wraps around" on 24 hours, it shouldn't
		assertEquals(5.0, link.getFreespeedTravelTime(30*3600.0), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(36*3600.0), EPSILON);
	}

	/**
	 * Tests whether a relative change in the freespeed really can be seen in the link's travel time
	 */
	public void testFreespeedChangeRelative() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);

		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink("1", "1", "2", "100", "10", "3600", "1", null, null);

		// test base values
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON);

		// add a relative change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		link.applyEvent(change);

		// do the tests for the actual value
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON); // at undefined time, return base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), EPSILON);  // one second before change, still base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 0.1), EPSILON);  // just before change, still base value
		assertEquals(5.0, link.getFreespeed(7*3600.0), EPSILON); // just on time of change, new value
		assertEquals(5.0, link.getFreespeed(8*3600.0), EPSILON); // some time later, still new value

		// do tests for derived values
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON); // and now the same tests for the travel time
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 1.0), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 0.1), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(7*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(8*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(24*3600.0), EPSILON); // also test if it "wraps around" on 24 hours, it shouldn't
		assertEquals(20.0, link.getFreespeedTravelTime(30*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(36*3600.0), EPSILON);
	}

	/**
	 * Tests how multiple freespeed changes interact with each other on the link.
	 */
	public void testMultipleFreespeedChanges() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);

		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink("1", "1", "2", "100", "10", "3600", "1", null, null);

		// test base values
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON);

		// add some changes:
		// - first a change event starting at 7am
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		link.applyEvent(change);
		// - second, a change event starting at from 8am
		NetworkChangeEvent change2 = new NetworkChangeEvent(8*3600.0);
		change2.addLink(link);
		change2.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 3.0));
		link.applyEvent(change2);
		// - third a change event starting at 10am
		NetworkChangeEvent change3 = new NetworkChangeEvent(10*3600.0);
		change3.addLink(link);
		change3.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 30));
		link.applyEvent(change3);

		/* I would now expect the following speeds:
		 * 0am-7am: 10
		 * 7am-8am: 20
		 * 8am-10am: 60
		 * 10am and later: 30
		 */

		// do the tests for the actual value
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON); // at undefined time, return base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), EPSILON);
		assertEquals(20.0, link.getFreespeed(7*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeed(8*3600.0-1), EPSILON);
		assertEquals(60.0, link.getFreespeed(8*3600.0), EPSILON);
		assertEquals(60.0, link.getFreespeed(10*3600.0-1), EPSILON);
		assertEquals(30.0, link.getFreespeed(10*3600.0), EPSILON);
		assertEquals(30.0, link.getFreespeed(18*3600.0), EPSILON);

		// everything fine so long, now add some more changes in a chronological arbitrary order
		
		// - a change event starting 12am
		NetworkChangeEvent change4 = new NetworkChangeEvent(12*3600.0);
		change4.addLink(link);
		change4.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 40));
		link.applyEvent(change4);
		// - a change event starting 11am
		NetworkChangeEvent change5 = new NetworkChangeEvent(11*3600.0);
		change5.addLink(link);
		change5.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		link.applyEvent(change5);
		// - a change event starting at 9am
		NetworkChangeEvent change6 = new NetworkChangeEvent(9*3600.0);
		change6.addLink(link);
		change6.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		link.applyEvent(change6);
		
		/* I would now expect the following speeds
		 * 0am-7am: 10
		 * 7am-8am: 20
		 * 8am-9am: 60
		 * 9am-10am: 30
		 * 10am-11am: 30
		 * 11am-12am: 15
		 * 12am and later: 40
		 * 
		 */

		// test again
		assertEquals(10.0, link.getFreespeed(6*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeed(7*3600.0), EPSILON);
		assertEquals(60.0, link.getFreespeed(8*3600.0), EPSILON);
		assertEquals(60.0, link.getFreespeed(9*3600.0-1), EPSILON);
		assertEquals(30.0, link.getFreespeed(9*3600.0), EPSILON);
		assertEquals(30.0, link.getFreespeed(10*3600.0-1), EPSILON);
		assertEquals(30.0, link.getFreespeed(10*3600.0), EPSILON);
		assertEquals(30.0, link.getFreespeed(11*3600.0-1), EPSILON);
		assertEquals(15.0, link.getFreespeed(11*3600.0), EPSILON);
		assertEquals(15.0, link.getFreespeed(12*3600.0-1), EPSILON);
		assertEquals(40.0, link.getFreespeed(12*3600.0), EPSILON);
		assertEquals(40.0, link.getFreespeed(18*3600.0), EPSILON);
	}

	/**
//	 * Tests whether an absolute change to the flow capacity really can be observed on the link when
//	 * no end-time for the change is specified.
//	 */
	public void disabled_testFlowCapChangeAbsoluteNoEnd() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink("1", "1", "2", "100", "10", "3600", "1", null, null);

		// test base values
		assertEquals(3600.0, link.getCapacity(), EPSILON);
		assertEquals(1.0, link.getFlowCapacity(), EPSILON);

		// add an absolute change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 7200));
		link.applyEvent(change);

		// do the tests
		fail("time-dependent getters are missing!");
		assertEquals(3600.0, link.getCapacity(), EPSILON);
		assertEquals(1.0, link.getFlowCapacity(), EPSILON);
		assertEquals(2.0, link.getFlowCapacity(), EPSILON);

		// test derived values
		// TODO test flowcap by sending vehicles through the link, this requires to create a queuenetwork from this time-variant network
	}

	// TODO additional tests for flowCap once there are useful implementations

	/**
	 * Tests whether an absolute change to the number of lanes really can be observed on the link when
	 * no end-time for the change is specified.
	 */
	public void disabled_testLanesChangeAbsoluteNoEnd() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink("1", "1", "2", "100", "10", "3600", "1", null, null);

		// test base values
		assertEquals(1.0, link.getLanes(), EPSILON);

		// add an absolute change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, 2.0));
		link.applyEvent(change);

		// do the tests
		fail("time-dependent getters are missing!");
		assertEquals(1.0, link.getLanes(), EPSILON);
		assertEquals(2.0, link.getLanes(), EPSILON);

		// test derived values
		// TODO e.g. test storage capacity on a queuelink based on this time-variant link
	}

	// TODO additional tests for lanes once there are useful implementations

}
