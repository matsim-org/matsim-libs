/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestColdEmissionEventImplementation.java                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.pt;


import java.util.LinkedList;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdHandler;
import playground.vsp.analysis.modules.ptOperator.TransitEventHandler;


//test for playground.vsp.analysis.modules.ptOperator

//Variablennamen vs Werte


public class TestTransitEventHandler extends TestCase{

	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	private long time;
	
	protected void setUp(){
		System.out.println("Starting " + super.getName());
		ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
		time = System.currentTimeMillis();
		
	}
	
	protected void tearDown(){
		time = System.currentTimeMillis()-time;
		System.out.println(super.getName() + " is done after " + time + "ms.");
	}
	
	@Rule public MatsimTestUtils utils= new MatsimTestUtils();
	@Test 
	public final void testHandleEventAgentArrival(){
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = (NetworkImpl) sc.getNetwork();
		Node node1 = network.createAndAddNode(sc.createId("1"), sc.createCoord(-20000.0,     0.0));
		Node node2 = network.createAndAddNode(sc.createId("2"), sc.createCoord(-17500.0,     0.0));
		network.createAndAddLink(sc.createId("1"), node1, node2, 1000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(sc.createId("2"), node1, node2, 1000, 27.78, 3600, 1, null, "23");

		//not using a config or controler 
		//=> not using the logger
		//=> no input or output files
		//=> no rundId needed
		//TODO write into decent documentation - later
		
		//create events
		LinkedList<AgentArrivalEvent> arrivalEvents = new LinkedList<AgentArrivalEvent>();
		LinkedList<AgentDepartureEvent> departureEvents = new LinkedList<AgentDepartureEvent>();
		LinkedList<TransitDriverStartsEvent> transitEvents = new LinkedList<TransitDriverStartsEvent>();
		
		createArrivalEvents(arrivalEvents);
		createDepartureEvents(departureEvents);
		createTransitEvents(transitEvents);
				
		//handle events
		PtDriverIdHandler pdih = new PtDriverIdHandler();
		pdih.reset(0);
		
		//init is not part of the abstract analysis module //TODO set link!
		//therefore it is tested here - empty initialization, double initialization
		//TODO ask Andreas about it - AbstractAnalysisModule is his
		//other methods are inherited from the abstract module -> not tested here
		
		try {
			ptDriverIdAnalyzer.init(null);
			ptDriverIdAnalyzer.init(sc);
			ptDriverIdAnalyzer.init(sc);
		} catch (Exception e1) {
			System.out.println("ptDriverIdAnalyzer was not initialized correctly. Tried to initilize it once empty, twice with the scenario.");
			Assert.fail("ptDriverIdAnalyzer was not initialized correctly");			
		}
		
		TransitEventHandler teh = new TransitEventHandler(network, ptDriverIdAnalyzer);
		teh.reset(0); //this does work as well without the reset... as long as it is the only transit event handler 
		//not resetting = improper use, not code failure 
	
		
		for(TransitDriverStartsEvent e: transitEvents){
			pdih.handleEvent(e);
			Assert.assertTrue("this event should be a pt event", ptDriverIdAnalyzer.isPtDriver(e.getDriverId()));
		}
		
		for(AgentDepartureEvent e: departureEvents){
			teh.handleEvent(e);
		}
		
		for(AgentArrivalEvent e: arrivalEvents){
			teh.handleEvent(e);
		}
		

		Double exp= .0 + aeTa-aeTd + noDTa-noDTd+ norTa-norTd //'bycar' should not be counted
		+ difLinks2Ta - difLinks2Td// + difLinks1Ta-difLinks1Td //this would  be for the second 'diffentent links' event but it doesnt count. why?
		//is this link initialized? //TODO look at this
		+ difPer1Ta-difPer1Td+ difPer2Ta-difPer2Td;
		exp= exp/3600.0; //seconds to hours
		
		Assert.assertFalse("by car is not public transport", ptDriverIdAnalyzer.isPtDriver(new IdImpl("by car")));
		
		LinkedList<String> ptList = new LinkedList<String>();
		
		
		ptList.add("50");ptList.add("zero duration");//ptList.add("negative duration");
		ptList.add("normal");
		ptList.add("different links"); //ptList.add("different persons"); ptList.add("different persons2");
		for(String id: ptList ){
			Assert.assertTrue("public transport", ptDriverIdAnalyzer.isPtDriver(new IdImpl(id)));
			if(!ptDriverIdAnalyzer.isPtDriver(new IdImpl(id))){
				//Log.info(id + " should be in the ptList but isnt." );
				System.out.println(id + " should be in the ptList but isnt." );
			}
		}
		Assert.assertEquals("total duration should be"+exp, exp, teh.getVehicleHours(), MatsimTestUtils.EPSILON);
	

	}
	
	//times for agent arrivals (Ta = Time for Arrival)
	int aeTa = 360, noDTa = 100, negDTa = 250, norTa = 3620, bycarTa = 100,
			difLinks2Ta = 900, difLinks1Ta = 900, //TODO avoid these multiples? or is there a reason for that?
			difPer1Ta = 450, difPer2Ta = 450;
	
	private void createArrivalEvents(LinkedList<AgentArrivalEvent> arrivalEvents) {
		IdImpl link1 = new IdImpl("1"); IdImpl link2 = new IdImpl("2");
		
		AgentArrivalEvent ae = new AgentArrivalEvent(aeTa, new IdImpl("50"), new IdImpl("13"), "pt");
		AgentArrivalEvent noD = new AgentArrivalEvent(noDTa, new IdImpl("zero duration"), link1, "pt");
		//TODO das muss im code geaendert werden, negative Zeiten sollen nicht gezaehlt werden
		//AgentArrivalEvent negD = new AgentArrivalEvent(negDTime, new IdImpl("negative duration"), link1, "pt"); 
		AgentArrivalEvent nor = new AgentArrivalEvent(norTa, new IdImpl("normal"), link1, "pt");
		AgentArrivalEvent bycar = new AgentArrivalEvent(bycarTa, new IdImpl("by car"), link1, "car");
		AgentArrivalEvent difLinks2 = new AgentArrivalEvent(difLinks2Ta, new IdImpl("different links"), link2, "pt");
		AgentArrivalEvent difLinks1 = new AgentArrivalEvent(difLinks1Ta, new IdImpl("different links"), link1, "pt");
		AgentArrivalEvent difPer1 = new AgentArrivalEvent(difPer1Ta, new IdImpl("different persons"), link1, "pt");
		AgentArrivalEvent difPer2 = new AgentArrivalEvent(difPer2Ta, new IdImpl("different persons2"), link1, "pt");
		
		arrivalEvents.add(ae); arrivalEvents.add(noD);
		//TODO das muss im Code geandert werden, siehe oben 
		//arrivalEvents.add(negD);
		arrivalEvents.add(nor);arrivalEvents.add(bycar);
		arrivalEvents.add(difLinks2);
		arrivalEvents.add(difLinks1);arrivalEvents.add(difPer1);arrivalEvents.add(difPer2);
	}
	
	//times for agent departures (Td = time for departure)
	int aeTd = 00, noDTd = 100, negDTd = 5000, norTd = 20, 
			bycarTd = 1180, difLinks1Td = 540, difLinks2Td = 540, difPer1Td = 126, difPer2Td = 126 ;
	
	private void createDepartureEvents(LinkedList<AgentDepartureEvent> departureEvents) {
		IdImpl link1 = new IdImpl("1"); IdImpl link2 = new IdImpl("2"); 
		
		//generate events
		AgentDepartureEvent ae = new AgentDepartureEvent(aeTd, new IdImpl("50"), new IdImpl("13"), "pt");
		AgentDepartureEvent noD = new AgentDepartureEvent(noDTd, new IdImpl("zero duration"), link1, "pt");
		//TODO siehe oben, negative Zeiten
		//AgentDepartureEvent negD = new AgentDepartureEvent(negDTd, new IdImpl("negative duration"), link1, "pt");
		AgentDepartureEvent nor = new AgentDepartureEvent(norTd, new IdImpl("normal"), link1, "pt");
		AgentDepartureEvent bycar = new AgentDepartureEvent(bycarTd, new IdImpl("by car"), link1, "car");
		AgentDepartureEvent difLinks1 = new AgentDepartureEvent(difLinks1Td, new IdImpl("different links"), link1, "pt");
		AgentDepartureEvent difLinks2 = new AgentDepartureEvent(difLinks2Td, new IdImpl("different links"), link2, "pt");
		AgentDepartureEvent difPer1 = new AgentDepartureEvent(difPer1Td, new IdImpl("different persons"), link1, "pt");
		AgentDepartureEvent difPer2 = new AgentDepartureEvent(difPer2Td, new IdImpl("different persons2"), link1, "pt");
		
		//add events to list
		departureEvents.add(ae); departureEvents.add(noD); 
		//TODO siehe oben, negative Zeiten
		//departureEvents.add(negD);
		departureEvents.add(nor);departureEvents.add(bycar);departureEvents.add(difLinks1);
		departureEvents.add(difLinks2);departureEvents.add(difPer1);departureEvents.add(difPer2);
		
	}	
	private void createTransitEvents(LinkedList<TransitDriverStartsEvent> transitEvents) {
		//TODO different vehicles
		
		IdImpl veh1 = new IdImpl("vehicle 1");
		//TODO wann bzgl der departure events muessen diese stattfinden?
		TransitDriverStartsEvent transitEvent = new TransitDriverStartsEvent(20, new IdImpl("50"), veh1, null, null, null);
		TransitDriverStartsEvent noD = new TransitDriverStartsEvent(100, new IdImpl("zero duration"), veh1, null, null, null);
		//TransitDriverStartsEvent negD = new TransitDriverStartsEvent(0, new IdImpl("negative duration"), veh1, null, null, null);
		TransitDriverStartsEvent nor = new TransitDriverStartsEvent(0, new IdImpl("normal"), veh1, null, null, null);
		TransitDriverStartsEvent bycar = new TransitDriverStartsEvent(0, new IdImpl("by car"), veh1, null, null, null);
		TransitDriverStartsEvent difLinks1 = new TransitDriverStartsEvent(540, new IdImpl("different links"), veh1, null, null, null);
		TransitDriverStartsEvent difLinks2 = new TransitDriverStartsEvent(540, new IdImpl("different links"), veh1, null, null, null);
		TransitDriverStartsEvent diffPer1 = new TransitDriverStartsEvent(226, new IdImpl("different persons"), veh1, null, null, null);
		TransitDriverStartsEvent diffPer2 = new TransitDriverStartsEvent(226, new IdImpl("different persons2"), veh1, null, null, null);
		transitEvents.add(transitEvent); 
		transitEvents.add(noD);//transitEvents.add(negD);
		transitEvents.add(nor); 
		//TODO alles hier wird automatisch als pt gezählt. ist das ok?
//		transitEvents.add(bycar); 
		transitEvents.add(difLinks1);
		transitEvents.add(difLinks2); transitEvents.add(diffPer1); transitEvents.add(diffPer2); 
		
	}
	/*
	@Test @Ignore
	public final void testHandleEventAgentDeparture(){
		
	}
	@Test @Ignore
	public final void testHandleEventLinkLeave(){
		
	}
	@Test @Ignore
	public final void testHandleEventTransitDriverStarts(){
		
	}
	@Test @Ignore
	public final void testGetVehicleHours(){
		
	}
	*/
	//TODO do i need to test the reset? no
	//(so far) no need to test getVehicleIds, getVehicleKm
	/*
 * AgentArrivalEvent aae = new AgentArrivalEvent(360, new IdImpl("50"), new IdImpl("13"), "pt");
		//LinkLeaveEvent lle = new LinkLeaveEvent(70, new IdImpl("50"), new IdImpl("13"), new IdImpl("vehicle1"));
		//needed to test for vehicle hours
		AgentDepartureEvent ade = new AgentDepartureEvent(00, new IdImpl("50"), new IdImpl("13"), "pt");
		
 * 
 * */
}

