/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.gershensonSignals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author droeder
 *
 */
public class CalculateSignalGroupsTest {
//	
//	final String INPUT = DaPaths.DASTUDIES + "denver\\";
//	Id id1 = new IdImpl("1");
//	Id id2 = new IdImpl("2");
//	Id id3 = new IdImpl("3");
//	Id id4 = new IdImpl("4");
//
//
//	ScenarioLoader  loader;
//	ScenarioImpl scenario;
//	Network net;
//	SortedMap<Id, SignalGroupDefinition> groups;
//	CalculateSignalGroups ccsg;
//	
//	private static final Logger log = Logger.getLogger(CalculateSignalGroupsTest.class);
// 
	@Before public void init() {
//		loader = new ScenarioLoaderImpl(INPUT + "denverConfig.xml");
//		scenario = (ScenarioImpl) loader.loadScenario();
//		net = scenario.getNetwork();
//		groups = scenario.getSignalSystems().getSignalGroupDefinitions();
//		ccsg = new CalculateSignalGroups(groups, net);
	}
	
	@Test public void testCorrespondingSignalGroups(){
//
//		SortedMap<Id, SignalGroupDefinition> groups = scenario.getSignalSystems().getSignalGroupDefinitions();
//		CalculateSignalGroups ccsg = new CalculateSignalGroups(groups, net);
//
//		ccsg.calculateCorrespondingGroups();
//		ccsg.calculateCompetingGroups(ccsg.calculateCorrespondingGroups());

//		assertEquals(id3 , ccsg.calculateCorrespondingGroups().
//				get(id1));
//		assertEquals(id1 , ccsg.calculateCorrespondingGroups().
//				get(id3));
//		assertEquals(id4 , ccsg.calculateCorrespondingGroups().
//				get(id2));
//		assertEquals(id2 , ccsg.calculateCorrespondingGroups().
//				get(id4));
//		assertEquals(new IdImpl("12"), ccsg.calculateMainOutlinks().get(new IdImpl("11")));
//		assertEquals(new IdImpl("14"), ccsg.calculateMainOutlinks().get(new IdImpl("13")));
//		assertEquals(new IdImpl("15"), ccsg.calculateMainOutlinks().get(new IdImpl("16")));
//		assertEquals(new IdImpl("17"), ccsg.calculateMainOutlinks().get(new IdImpl("18")));
		
//		ccsg.calcCorrGroups();
		
		
//		assertEquals(1.5*Math.PI, ccsg.calcAngle(net.getLinks().get(new IdImpl("14"))), 0);
	}
	@After
	public void endTest(){
		
	}

}

