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
package playground.droeder.analysis.trips;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.droeder.Analysis.Trips.AnalysisTripGenerator;
import playground.droeder.Analysis.Trips.AnalysisTripSetOneMode;

/**
 * @author droeder
 *
 */
public class AnalysisTripGeneratorTest {
	private Map<Id, ArrayList<PersonEvent>> personEvents;
	private Map<Id, ArrayList<PlanElement>> planElements;
	private Map<String, AnalysisTripSetOneMode> sets;
	
	
	@Before
	public void init(){
		this.personEvents = new HashMap<Id, ArrayList<PersonEvent>>();
		this.planElements = new HashMap<Id, ArrayList<PlanElement>>();
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Id agentId, link1, link2, link3, link4, fac1, fac2, fac3, fac4;
		PopulationFactory fac = sc.getPopulation().getFactory();
		PlanElement pe;
		PersonEvent event;
		String type1, type2, type3, mode1, mode2;
		ArrayList<PersonEvent> events ;
		ArrayList<PlanElement> elements;
		
		link1 = sc.createId("l1");
		link4 = sc.createId("l4");
		fac1 = sc.createId("f1");
		fac4 = sc.createId("f4");
		type1 = "home";
		type3 = "work";
		mode1 = TransportMode.car;

		//create carPlan
		for(int i = 0; i<2; i++){
			events = new ArrayList<PersonEvent>();
			elements = new ArrayList<PlanElement>();
			agentId = sc.createId(String.valueOf(i));
			
			event = new ActivityEndEventImpl(0.0, agentId, link1, fac1, type1);
			events.add(event);
			pe = fac.createActivityFromLinkId(type1, link1);
			elements.add(pe);
			
			event = new AgentDepartureEventImpl(1.0, agentId, link1, mode1);
			events.add(event);
			pe = fac.createLeg(mode1);
			elements.add(pe);
			
			event = new AgentArrivalEventImpl(10.0, agentId, link4, mode1);
			events.add(event);
			
			event = new ActivityStartEventImpl(11.0, agentId, link4, fac4, type3);
			events.add(event);
			pe = fac.createActivityFromLinkId(type1, link1);
			elements.add(pe);
			
			this.personEvents.put(agentId, events);
			this.planElements.put(agentId, elements);
		}
		
		//create PtPlan
		
		link2 = sc.createId("l2");
		link3 = sc.createId("l3");
		fac2 = sc.createId("fac2");
		fac3 = sc.createId("f3");
		type1 = "home";
		type2 = "pt interaction";
		type3 = "work";
		mode1 = TransportMode.transit_walk;
		mode2 = TransportMode.pt;
		
		for(int i = 2; i < 4; i++){
			events = new ArrayList<PersonEvent>();
			elements = new ArrayList<PlanElement>();
			agentId = sc.createId(String.valueOf(i));
			
			event = new ActivityEndEventImpl(0.0, agentId, link1, fac1, type1);
			events.add(event);
			pe = fac.createActivityFromLinkId(type1, link1);
			elements.add(pe);
			
			event = new AgentDepartureEventImpl(1.0, agentId, link1, mode1);
			events.add(event);
			pe = fac.createLeg(mode1);
			elements.add(pe);
			event = new AgentArrivalEventImpl(10.0, agentId, link2, mode1);
			events.add(event);
			
			event = new ActivityStartEventImpl(11.0, agentId, link2, fac2, type2);
			events.add(event);
			pe = fac.createActivityFromLinkId(type2, link2);
			elements.add(pe);
			event = new ActivityEndEventImpl(13.0, agentId, link2, fac2, type2);
			events.add(event);
			
			event = new AgentDepartureEventImpl(14.0, agentId, link2, mode2);
			events.add(event);
			pe = fac.createLeg(mode2);
			elements.add(pe);
			event = new AgentArrivalEventImpl(20.0, agentId, link3, mode2);
			events.add(event);
			
			event = new ActivityStartEventImpl(21.0, agentId, link3, fac3, type2);
			events.add(event);
			pe = fac.createActivityFromLinkId(type2, link2);
			elements.add(pe);
			event = new ActivityEndEventImpl(22.0, agentId, link3, fac3, type2);
			events.add(event);
			
			event = new AgentDepartureEventImpl(23.0, agentId, link3, mode1);
			events.add(event);
			pe = fac.createLeg(mode1);
			elements.add(pe);
			event = new AgentArrivalEventImpl(30.0, agentId, link4, mode1);
			events.add(event);
			
			event = new ActivityStartEventImpl(31.0, agentId, link4, fac4, type3);
			events.add(event);
			pe = fac.createActivityFromLinkId(type3, link4);
			elements.add(pe);
			
			this.personEvents.put(agentId, events);
			this.planElements.put(agentId, elements);
		}
		
		this.sets = AnalysisTripGenerator.calculateTripSet(this.personEvents, this.planElements, null, false).getTripSets();
	}

	@Test
	public void testCalculateTripSetAllModes(){
		AnalysisTripSetOneMode set = this.sets.get(TransportMode.car);
		
		assertEquals("wrong TTime", 18.0, set.getSumTTime()[0], 0.0);
		assertEquals("wrong avTTime", 9.0, set.getAvTripTTime()[0], 0.0);
		assertEquals("wrong TripCnt", 2.0, set.getTripCnt()[0], 0.0);
		
	}
	
	@Test
	public void testCalculateTripSetPt(){
		AnalysisTripSetOneMode set = this.sets.get(TransportMode.pt);
		
		assertEquals("wrong TTime", 58.0, set.getSumTTime()[0], 0.0);
		assertEquals("wrong avTTime", 29.0, set.getAvTripTTime()[0], 0.0);
		assertEquals("wrong TripCnt", 2.0, set.getTripCnt()[0], 0.0);
		
		assertEquals("wrong AccesWaitCount", 2.0, set.getAccesWaitCnt()[0], 0.0);
		assertEquals("wrong AccesWaitTime", 4.0, set.getAccesWaitTime()[0], 0.0);
		assertEquals("wrong avAccesWaitTime", 2.0, set.getAvAccesWaitTime()[0], 0.0);
		
		assertEquals("wrong AccesWalkCount", 2.0, set.getAccesWalkCnt()[0], 0.0);
		assertEquals("wrong AccesWalkTime", 18.0, set.getAccesWalkTTime()[0], 0.0);
		assertEquals("wrong avAccesWalkTime", 9.0, set.getAvAccesWalkTTime()[0], 0.0);
		
		assertEquals("wrong eggresWalkCount", 2.0, set.getEgressWalkCnt()[0], 0.0);
		assertEquals("wrong eggresWalkTTime", 14.0, set.getEgressWalkTTime()[0], 0.0);
		assertEquals("wrong avEggresWalkTTime", 7.0, set.getAvEgressWalkTTime()[0], 0.0);
		
		assertEquals("wrong switchWalkCount", 0.0, set.getSwitchWalkCnt()[0], 0.0);
		assertEquals("wrong switchWalkTTime", 0.0, set.getSwitchWalkTTime()[0], 0.0);
		assertEquals("wrong avSwitchWalkTTime", Double.NaN, set.getAvSwitchWalkTTime()[0], 0.0);
		
		assertEquals("wrong switchWaitCount", 0.0, set.getSwitchWaitCnt()[0], 0.0);
		assertEquals("wrong switchWaitTime", 0.0, set.getSwitchWaitTime()[0], 0.0);
		assertEquals("wrong avSwitchWaitTime", Double.NaN, set.getAvSwitchWaitTime()[0], 0.0);
		
		assertEquals("wrong lineCount", 2.0, set.getLineCnt()[0], 0.0);
		assertEquals("wrong lineTTime", 12.0, set.getLineTTime()[0], 0.0);
		assertEquals("wrong avLineTTime", 6.0, set.getAvLineTTime()[0], 0.0);
		
		assertEquals("wrong switch0Cnt", 2, set.getSwitch0cnt()[0], 0.0);
		assertEquals("wrong switch1Cnt", 0, set.getSwitch1cnt()[0], 0.0);
		assertEquals("wrong switch2Cnt", 0, set.getSwitch2cnt()[0], 0.0);
		assertEquals("wrong switchGt2Cnt", 0, set.getSwitchGT2cnt()[0], 0.0);
		
		assertEquals("wrong fSwitch0", 1, set.getfSwitch0()[0], 0.0);
		assertEquals("wrong fSwitch1", 0, set.getfSwitch1()[0], 0.0);
		assertEquals("wrong fSwitch2", 0, set.getfSwitch2()[0], 0.0);
		assertEquals("wrong fSwitchGt2", 0, set.getfSwitchGT2()[0], 0.0);
		
		assertEquals("wrong line1Cnt", 2, set.getSwitch0cnt()[0], 0.0);
		assertEquals("wrong line2Cnt", 0, set.getSwitch1cnt()[0], 0.0);
		assertEquals("wrong line3Cnt", 0, set.getSwitch2cnt()[0], 0.0);
		assertEquals("wrong lineGt3Cnt", 0, set.getSwitchGT2cnt()[0], 0.0);
		
		assertEquals("wrong fLine1", 1, set.getfSwitch0()[0], 0.0);
		assertEquals("wrong fLine2", 0, set.getfSwitch1()[0], 0.0);
		assertEquals("wrong fLine3", 0, set.getfSwitch2()[0], 0.0);
		assertEquals("wrong fLineGt3", 0, set.getfSwitchGT2()[0], 0.0);
	}
	
	@After
	public void endTest(){
		
	}
	
}
