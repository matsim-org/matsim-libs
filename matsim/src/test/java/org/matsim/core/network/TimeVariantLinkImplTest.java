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

package org.matsim.core.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 * @author laemmel
 */
public class TimeVariantLinkImplTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final double TIME_BEFORE_FIRST_CHANGE_EVENTS = -99999;//when  base (default) link properties are used

	/** Tests the method {@link NetworkUtils#getFreespeedTravelTime(Link, double)}.	 */
	@Test
	void testGetFreespeedTravelTime(){
	    for (LinkFactory lf : linkFactories(1, 5)) {
    		final Network network = new NetworkImpl(lf);
    		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
    		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
    		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 2000, (double) 2000));
		final Node fromNode = node1;
		final Node toNode = node2;
    		final Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, 1.667, (double) 3600, (double) 1 );
		final Node fromNode1 = node3;
		final Node toNode1 = node4;
    		final Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode1, toNode1, (double) 1000, 1.667, (double) 3600, (double) 1 );

    		final double [] queryDates = {TIME_BEFORE_FIRST_CHANGE_EVENTS, 0., 1., 2., 3., 4.};

    		// link1 change event absolute, undef. endtime
    		final double [] responsesLink1 = {1.667, 1.667, 10., 10., 10., 10.};
    		NetworkChangeEvent event = new NetworkChangeEvent(1);
    		event.addLink(link1);
    		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,10.));
    		((TimeVariantLinkImpl)link1).applyEvent(event);

    		// link3 change event factor, undef. endtime
    		final double [] responsesLink3 = {1.667, 1.667, 10.002, 10.002, 10.002, 10.002};
    		event = new NetworkChangeEvent(1);
    		event.addLink(link3);
    		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,6.));
    		((TimeVariantLinkImpl)link3).applyEvent(event);

    		for (int i = 0; i < queryDates.length; i++) {
    			assertEquals(responsesLink1[i], link1.getFreespeed(queryDates[i]), MatsimTestUtils.EPSILON);
    			assertEquals(responsesLink3[i], link3.getFreespeed(queryDates[i]), MatsimTestUtils.EPSILON);
    		}
	    }
	}

	/**
	 * Tests whether an absolute change in the freespeed really can be seen in the link's travel time
	 */
	@Test
	void testFreespeedChangeAbsolute() {
        for (LinkFactory lf : linkFactories(15 * 60, 30 * 3600)) {
        	final Network network = new NetworkImpl(lf);

    		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
    		TimeVariantLinkImpl link = (TimeVariantLinkImpl)NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 10, (double) 3600, (double) 1 );

    		// test base values
    		assertEquals(10.0, link.getFreespeed(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);

    		// add an absolute change
    		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
    		change.addLink(link);
    		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 20));
    		link.applyEvent(change);

    		// do the tests
    		assertEquals(10.0, link.getFreespeed(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON); // before first change event, return base value
    		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), MatsimTestUtils.EPSILON);  // one second before change, still base value
    		assertEquals(10.0, link.getFreespeed(7*3600.0 - 0.1), MatsimTestUtils.EPSILON);  // just before change, still base value
    		assertEquals(20.0, link.getFreespeed(7*3600.0), MatsimTestUtils.EPSILON); // just on time of change, new value
    		assertEquals(20.0, link.getFreespeed(8*3600.0), MatsimTestUtils.EPSILON); // some time later, still new value

    		// test derived values
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON); // and now the same tests for the travel time
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, 7*3600.0 - 1.0), MatsimTestUtils.EPSILON);
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, 7*3600.0 - 0.1), MatsimTestUtils.EPSILON);
    		assertEquals(5.0, NetworkUtils.getFreespeedTravelTime(link, 7*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(5.0, NetworkUtils.getFreespeedTravelTime(link, 8*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(5.0, NetworkUtils.getFreespeedTravelTime(link, 24*3600.0), MatsimTestUtils.EPSILON); // also test if it "wraps around" on 24 hours, it shouldn't
    		assertEquals(5.0, NetworkUtils.getFreespeedTravelTime(link, 30*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(5.0, NetworkUtils.getFreespeedTravelTime(link, 36*3600.0), MatsimTestUtils.EPSILON);
        }
	}

	/**
	 * Tests whether a relative change in the freespeed really can be seen in the link's travel time
	 */
	@Test
	void testFreespeedChangeRelative() {
        for (LinkFactory lf : linkFactories(15 * 60, 30 * 3600)) {
        	final Network network = new NetworkImpl(lf);

    		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
    		TimeVariantLinkImpl link = (TimeVariantLinkImpl)NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 10, (double) 3600, (double) 1 );

    		// test base values
    		assertEquals(10.0, link.getFreespeed(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);

    		// add a relative change
    		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
    		change.addLink(link);
    		change.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
    		link.applyEvent(change);

    		// do the tests for the actual value
    		assertEquals(10.0, link.getFreespeed(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON); // before first change event, return base value
    		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), MatsimTestUtils.EPSILON);  // one second before change, still base value
    		assertEquals(10.0, link.getFreespeed(7*3600.0 - 0.1), MatsimTestUtils.EPSILON);  // just before change, still base value
    		assertEquals(5.0, link.getFreespeed(7*3600.0), MatsimTestUtils.EPSILON); // just on time of change, new value
    		assertEquals(5.0, link.getFreespeed(8*3600.0), MatsimTestUtils.EPSILON); // some time later, still new value

    		// do tests for derived values
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON); // and now the same tests for the travel time
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, 7*3600.0 - 1.0), MatsimTestUtils.EPSILON);
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, 7*3600.0 - 0.1), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, NetworkUtils.getFreespeedTravelTime(link, 7*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, NetworkUtils.getFreespeedTravelTime(link, 8*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, NetworkUtils.getFreespeedTravelTime(link, 24*3600.0), MatsimTestUtils.EPSILON); // also test if it "wraps around" on 24 hours, it shouldn't
    		assertEquals(20.0, NetworkUtils.getFreespeedTravelTime(link, 30*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, NetworkUtils.getFreespeedTravelTime(link, 36*3600.0), MatsimTestUtils.EPSILON);
	    }
	}

	/**
	 * Tests how multiple freespeed changes interact with each other on the link.
	 */
	@Test
	void testMultipleFreespeedChanges() {
        for (LinkFactory lf : linkFactories(15 * 60, 30 * 3600)) {
        	final Network network = new NetworkImpl(lf);

    		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
    		TimeVariantLinkImpl link = (TimeVariantLinkImpl)NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 10, (double) 3600, (double) 1 );

    		// test base values
    		assertEquals(10.0, link.getFreespeed(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(10.0, NetworkUtils.getFreespeedTravelTime(link, TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);

    		// add some changes:
    		// - first a change event starting at 7am
    		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
    		change.addLink(link);
    		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 20));
    		link.applyEvent(change);
    		// - second, a change event starting at from 8am
    		NetworkChangeEvent change2 = new NetworkChangeEvent(8*3600.0);
    		change2.addLink(link);
    		change2.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 3.0));
    		link.applyEvent(change2);
    		// - third a change event starting at 10am
    		{
    		NetworkChangeEvent change3 = new NetworkChangeEvent(10*3600.0);
    		change3.addLink(link);
    		change3.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 30));
    		link.applyEvent(change3);
    		}
    		// - ...
    		{
    		NetworkChangeEvent change4 = new NetworkChangeEvent(19*3600.0);
    		change4.addLink(link);
    		change4.setFreespeedChange(new ChangeValue(ChangeType.OFFSET_IN_SI_UNITS, +22.));
    		link.applyEvent(change4);
    		}

    		/* I would now expect the following speeds:
    		 * 0am-7am: 10
    		 * 7am-8am: 20
    		 * 8am-10am: 60
    		 * 10am and later: 30
    		 */

    		// do the tests for the actual value
    		assertEquals(10.0, link.getFreespeed(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON); // before first change event, return base value
    		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, link.getFreespeed(7*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, link.getFreespeed(8*3600.0-1), MatsimTestUtils.EPSILON);
    		assertEquals(60.0, link.getFreespeed(8*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(60.0, link.getFreespeed(10*3600.0-1), MatsimTestUtils.EPSILON);
    		assertEquals(30.0, link.getFreespeed(10*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(30.0, link.getFreespeed(18*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(52.0, link.getFreespeed(19.1*3600.0), MatsimTestUtils.EPSILON);

    		// everything fine so long, now add some more changes in a chronological arbitrary order

    		// - a change event starting 12am
    		NetworkChangeEvent change4 = new NetworkChangeEvent(12*3600.0);
    		change4.addLink(link);
    		change4.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 40));
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
    		assertEquals(10.0, link.getFreespeed(6*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(20.0, link.getFreespeed(7*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(60.0, link.getFreespeed(8*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(60.0, link.getFreespeed(9*3600.0-1), MatsimTestUtils.EPSILON);
    		assertEquals(30.0, link.getFreespeed(9*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(30.0, link.getFreespeed(10*3600.0-1), MatsimTestUtils.EPSILON);
    		assertEquals(30.0, link.getFreespeed(10*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(30.0, link.getFreespeed(11*3600.0-1), MatsimTestUtils.EPSILON);
    		assertEquals(15.0, link.getFreespeed(11*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(15.0, link.getFreespeed(12*3600.0-1), MatsimTestUtils.EPSILON);
    		assertEquals(40.0, link.getFreespeed(12*3600.0), MatsimTestUtils.EPSILON);
    		assertEquals(40.0, link.getFreespeed(18*3600.0), MatsimTestUtils.EPSILON);
        }
	}

	/**
	 * Tests whether an absolute change to the flow capacity really can be observed on the link .
	 */
	@Test
	void testFlowCapChangeAbsolute() {
        for (LinkFactory lf : linkFactories(15 * 60, 30 * 3600)) {
        	final Network network = new NetworkImpl(lf);
    		network.setCapacityPeriod(3600.0);

    		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
    		TimeVariantLinkImpl link = (TimeVariantLinkImpl)NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 10, (double) 3600, (double) 1 );

    		// test base values
    		assertEquals(3600.0, link.getCapacity(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(1.0, link.getFlowCapacityPerSec(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);

    		// add an absolute change
    		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
    		change.addLink(link);
    		change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 2));
    		link.applyEvent(change);

    		// do the tests
    		assertEquals(3600.0, link.getCapacity(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(1.0, link.getFlowCapacityPerSec(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(2.0, link.getFlowCapacityPerSec(7*3600), MatsimTestUtils.EPSILON);

    		// test derived values
    		// TODO test flowcap by sending vehicles through the link, this requires to create a queuenetwork from this time-variant network
        }
	}

	// TODO additional tests for flowCap once there are useful implementations

	/**
	 * Tests whether an absolute change to the number of lanes really can be observed on the link.
	 */
	@Test
	void testLanesChangeAbsolute() {
        for (LinkFactory lf : linkFactories(15 * 60, 30 * 3600)) {
        	final Network network = new NetworkImpl(lf);
    		network.setCapacityPeriod(3600.0);

    		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
    		TimeVariantLinkImpl link = (TimeVariantLinkImpl)NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 10, (double) 3600, (double) 1 );

    		// test base values
    		assertEquals(1.0, link.getNumberOfLanes(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);

    		// add an absolute change
    		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
    		change.addLink(link);
    		change.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 2.0));
    		link.applyEvent(change);

    		// do the tests
    		assertEquals(1.0, link.getNumberOfLanes(TIME_BEFORE_FIRST_CHANGE_EVENTS), MatsimTestUtils.EPSILON);
    		assertEquals(2.0, link.getNumberOfLanes(7*3600), MatsimTestUtils.EPSILON);

    		// test derived values
    		// TODO e.g. test storage capacity on a queuelink based on this time-variant link
        }
	}


    static LinkFactory[] linkFactories(int interval, int maxTime)
    {
        return new LinkFactory[] {
            new VariableIntervalTimeVariantLinkFactory(),
            new FixedIntervalTimeVariantLinkFactory(interval, maxTime)
        };
    }



	//////////////////////////////////////////////////////////////////////
	// Time variant tests on QueueLink
	//////////////////////////////////////////////////////////////////////

	// I moved those to core.mobsim.queuesim so that I can further reduce visibility of some queuesimu internals. kai, may'10

}
