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

import org.apache.log4j.Logger;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.testcases.MatsimTestCase;

public class TimeVariantLinkImplTest extends MatsimTestCase {
	
	
	private final static Logger log = Logger.getLogger(TimeVariantLinkImplTest.class);
	
	/** Tests the method {@link TimeVariantLinkImpl#getFreespeedTravelTime(double)}.
	 *  
	 * @author laemmel
	 */
	public void testGetFreespeedTravelTime(){
		
		log.info("running testGetFreespeedTravelTime()...");
		final double  epsilon = 1e-10;
		
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.createNode("1", "0", "0", null);
		network.createNode("2", "0", "1000", null);
		network.createNode("3", "1000", "2000", null);
		network.createNode("4", "2000", "2000", null);
		network.createNode("5", "1000", "0", null);
		final Link link1 = network.createLink("1", "1", "2", "1000", "1.667", "3600", "1", null, null);
		final Link link2 = network.createLink("2", "2", "3", "1500", "1.667", "3600", "1", null, null);
		final Link link3 = network.createLink("3", "3", "4", "1000", "1.667", "3600", "1", null, null);
		final Link link4 = network.createLink("4", "4", "5", "2800", "1.667", "3600", "1", null, null);
		
		final double [] queryDates = {org.matsim.utils.misc.Time.UNDEFINED_TIME, 0., 1., 2., 3., 4.};
		
		// link1 change event absolute, undef. endtime
		final double [] responsesLink1 = {1.667, 1.667, 10., 10., 10., 10.};
		NetworkChangeEvent event = new NetworkChangeEvent(1);
		event.addLink(link1);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,10.));
		((TimeVariantLinkImpl)link1).applyEvent(event);

		// link2 change event absolute, defined endtime
		final double [] responsesLink2 = {1.667, 1.667, 10., 10., 10., 1.667};
		event = new NetworkChangeEvent(1);
		event.addLink(link2);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,10.));
		event.setEndTime(4);
		((TimeVariantLinkImpl)link2).applyEvent(event);

		// link3 change event factor, undef. endtime
		final double [] responsesLink3 = {1.667, 1.667, 10.002, 10.002, 10.002, 10.002};
		event = new NetworkChangeEvent(1);
		event.addLink(link3);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,6.));
		((TimeVariantLinkImpl)link3).applyEvent(event);

		// link4 change event factor, defined endtime
		final double [] responsesLink4 = {1.667, 1.667, 10.002, 10.002, 10.002, 1.667};
		event = new NetworkChangeEvent(1);
		event.addLink(link4);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,6.));
		event.setEndTime(4);
		((TimeVariantLinkImpl)link4).applyEvent(event);
		
		for (int i = 0; i < queryDates.length; i++) {
			
			assertEquals(responsesLink1[i], link1.getFreespeed(queryDates[i]),epsilon);
			assertEquals(responsesLink2[i], link2.getFreespeed(queryDates[i]),epsilon);
			assertEquals(responsesLink3[i], link3.getFreespeed(queryDates[i]),epsilon);
			assertEquals(responsesLink4[i], link4.getFreespeed(queryDates[i]),epsilon);
			
		}
		
		log.info("done.");
	}

}
