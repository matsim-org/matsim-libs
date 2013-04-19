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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdHandler;
import playground.vsp.analysis.modules.ptOperator.TransitEventHandler;


/*
 * test for playground.vsp.analysis.modules.ptOperator
 * @link playground.vsp.analysis.modules.ptOperator
 * 
 * @author julia
 */

//TODO Variablennamen vs Werte
//TODO explain  which cases are covered/tested
//not covered: link not in network
//TODO departure ohne arrival?
//TODO do i need to test the reset? no
//(so far) no need to test getVehicleIds, getVehicleKm



public class TestTransitEventHandler extends TestCase{

	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	private long time;
	private String pt = "pt"; //name for public transport mode
	private String car = "car"; //other mode. no need to be implemented - TODO try null, "", "abcde"
	
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
		Node node1 = network.createAndAddNode(sc.createId("node 1"), sc.createCoord(-20000.0,     0.0));
		Node node2 = network.createAndAddNode(sc.createId("node 2"), sc.createCoord(-17500.0,     0.0));
		network.createAndAddLink(sc.createId(link1N), node1, node2, 1000, 27.78, 3600, 1, null, null); //link type was "22" but is probably not needed here
		network.createAndAddLink(sc.createId(link2N), node1, node2, 1000, 27.78, 3600, 1, null, ""); //link type was "22" but is probably not needed here
		network.createAndAddLink(sc.createId(link3N), node2, node1, 0, 30, 4000, 1); //TODO zero length! shall we test this? -> Ihab

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
		//	Assert.assertTrue("this event should be a pt event", ptDriverIdAnalyzer.isPtDriver(e.getDriverId()));
			
		}
		
		for(AgentDepartureEvent e: departureEvents){
			teh.handleEvent(e);
		}
		
		for(AgentArrivalEvent e: arrivalEvents){
			teh.handleEvent(e);
		}
		


		
		LinkedList<String> ptList = new LinkedList<String>();
		
		//create manually a list of all pt events to check 'isPtDriverMethod'
		ptList.add(aeN);ptList.add(noDN);//ptList.add(negDN); //TODO
		ptList.add(norN);
		ptList.add(difLinks1N); //ptList.add(difPer1N); ptList.add(difPer2N); //TODO
		
		//test: is no pt-event missing?
		//does 'isPtDriver' work correctly on pt events?
		for(String id: ptList ){
			Assert.assertTrue("this is public transport event but was not recognized as one.", ptDriverIdAnalyzer.isPtDriver(new IdImpl(id)));
			if(!ptDriverIdAnalyzer.isPtDriver(new IdImpl(id))){
				System.out.println(id + " should be in the ptList but is not. Failure at ptDriverIdAnalyzer.isPtDriver" );
			}
		}
		//hat der ptDriverAnalyzer mehr ptevents als er haben sollte?
		//muessen nur noch laenge vergleichen mit dem test oben sind ja alle drin, die drin sein sollen
		//waeren falsche elemete drin, muesste die liste also laenger sein
		//TODO ptDriverPrefixHandler ist private ... kann ich trotzdem irgendwie an die Menge aller PtDriver kommen? -> Ihab
		//sonst ist das aber vermutlich ok so, hier wird ja noch getested, dass 'bycar' nicht faelschlich aufgenommen wird
				
		Assert.assertFalse("'by car' is not public transport event but was recognized as one.", ptDriverIdAnalyzer.isPtDriver(new IdImpl(bycarN)));
		
		//test TransitEventHandler.getVehicleHours
		//manually calculate the expected value
		Double exp= .0 + aeTa-aeTd + noDTa-noDTd+ norTa-norTd 
				//'bycar' should not be counted since it is not pt
		+ difLinks2Ta - difLinks2Td
		// + difLinks1Ta-difLinks1Td //this would  be for the second 'diffentent links' event but it doesnt count. why?
		//is this link initialized? //TODO look at this
		+ difPer1Ta-difPer1Td+ difPer2Ta-difPer2Td;
		exp= exp/3600.0; //seconds to hours
		
		Assert.assertEquals("total duration should be"+exp, exp, teh.getVehicleHours(), MatsimTestUtils.EPSILON);
	

	}
	

	
	//TODO -> Ihab: gleiche Strings oder gleiche IdImpl verwenden?
	//names for agent ids 
	String aeN = "50", noDN = "zero duration", negDN = "negative duration", norN = "normal", 
			bycarN ="by car", 
			difLinks2N = "different links", difLinks1N = difLinks2N, //same agent on different links 
			//TODO ist das gleich, wenn ich jeweils neue id-impl mache?
			//alternativ unten nur eine idimpl erzeugen
			difPer1N = "different persons", difPer2N = "different persons2";
	
	//times for agent arrivals (Ta = Time for Arrival)
	int aeTa = 360, noDTa = 100, negDTa = 250, norTa = 3620, bycarTa = 100,
			difLinks2Ta = 900, difLinks1Ta = 900, //TODO avoid these multiples? or is there a reason for that?
			difPer1Ta = 450, difPer2Ta = 450;
	
		//times for agent departures (Td = time for departure)
	int aeTd = 00, noDTd = 100, negDTd = 5000, norTd = 20, 
			bycarTd = 1180, difLinks1Td = 540, difLinks2Td = 540, difPer1Td = 126, difPer2Td = 126 ;
	
		//times for transit start (Ts = time for transit start)		//TODO wann bzgl der departure events muessen diese stattfinden?
	//TODO or should i use double? 
	int aeTs = 20, noDTs = 100, negDTs = 0, norTs = 0, difLinks1Ts = 540, difLinks2Ts = 540, difPer1Ts = 226, difPer2Ts = 226;
	
	//names for links
	//TODO -> Ihab fragen: wie oben, jeweils neue idimpl beim erzeugen der events (bisher). wollen wir lieber die gleiche idimpl fuer alle/alles?
	String link1N = "link1", link2N = "link2", link3N = "link3";
	
	//names for vehicles
	String veh1N = "vehicle 1", veh2N = "vehicle 2";
	
	private void createArrivalEvents(LinkedList<AgentArrivalEvent> arrivalEvents) {

		IdImpl link1 = new IdImpl(link1N), link2 = new IdImpl(link2N), link3 = new IdImpl(link3N);
		
		//create arrival events
		AgentArrivalEvent ae = new AgentArrivalEvent(aeTa, new IdImpl(aeN), new IdImpl(link1N), pt); //new IdImpl of the same name should lead to the same result as using the same implementation
		AgentArrivalEvent noD = new AgentArrivalEvent(noDTa, new IdImpl(noDN), link1, pt);
		AgentArrivalEvent negD = new AgentArrivalEvent(negDTa, new IdImpl(negDN), link1, pt); //not used yet, see below
		AgentArrivalEvent nor = new AgentArrivalEvent(norTa, new IdImpl(norN), link1, pt);
		AgentArrivalEvent bycar = new AgentArrivalEvent(bycarTa, new IdImpl(bycarN), link1, car);
		AgentArrivalEvent difLinks2 = new AgentArrivalEvent(difLinks2Ta, new IdImpl(difLinks1N), link2, pt);
		AgentArrivalEvent difLinks1 = new AgentArrivalEvent(difLinks1Ta, new IdImpl(difLinks2N), link1, pt);
		AgentArrivalEvent difPer1 = new AgentArrivalEvent(difPer1Ta, new IdImpl(difPer1N), link1, pt);
		AgentArrivalEvent difPer2 = new AgentArrivalEvent(difPer2Ta, new IdImpl(difPer2N), link1, pt);
		
		//add events to list
		arrivalEvents.add(ae); arrivalEvents.add(noD);
		//TODO das muss im code geaendert werden, negative Zeiten sollen nicht gezaehlt werden
		//arrivalEvents.add(negD);
		arrivalEvents.add(nor);arrivalEvents.add(bycar);
		arrivalEvents.add(difLinks2); arrivalEvents.add(difLinks1);
		arrivalEvents.add(difPer1);arrivalEvents.add(difPer2);
	}
	

	
	private void createDepartureEvents(LinkedList<AgentDepartureEvent> departureEvents) {
		IdImpl link1 = new IdImpl(link1N), link2 = new IdImpl(link2N), link3 = new IdImpl(link3N); 
		
		//create departure events (time, agent id, leg id, legMode)
		AgentDepartureEvent ae = new AgentDepartureEvent(aeTd, new IdImpl(aeN), new IdImpl(link1N), pt); //new IdImpl of the same name should lead to the same result as using the same implementation
		AgentDepartureEvent noD = new AgentDepartureEvent(noDTd, new IdImpl(noDN), link1, pt);
		AgentDepartureEvent negD = new AgentDepartureEvent(negDTd, new IdImpl(negDN), link1, pt); //not used yet, see above
		AgentDepartureEvent nor = new AgentDepartureEvent(norTd, new IdImpl(norN), link1, pt);
		AgentDepartureEvent bycar = new AgentDepartureEvent(bycarTd, new IdImpl(bycarN), link1, car);
		AgentDepartureEvent difLinks1 = new AgentDepartureEvent(difLinks1Td, new IdImpl(difLinks1N), link1, pt);
		AgentDepartureEvent difLinks2 = new AgentDepartureEvent(difLinks2Td, new IdImpl(difLinks2N), link2, pt);
		AgentDepartureEvent difPer1 = new AgentDepartureEvent(difPer1Td, new IdImpl(difPer1N), link1, pt);
		AgentDepartureEvent difPer2 = new AgentDepartureEvent(difPer2Td, new IdImpl(difPer2N), link1, pt);
		
		//add events to list
		departureEvents.add(ae); departureEvents.add(noD); 
		//TODO siehe oben, negative Zeiten
		//departureEvents.add(negD);
		departureEvents.add(nor);departureEvents.add(bycar);departureEvents.add(difLinks1);
		departureEvents.add(difLinks2);departureEvents.add(difPer1);departureEvents.add(difPer2);
		
	}	
	private void createTransitEvents(LinkedList<TransitDriverStartsEvent> transitEvents) {
		//TODO different vehicles
		IdImpl veh1 = new IdImpl(veh1N);
		
		//create transit events (time, driverId, vehicleId, transitLineId, transitRouteId, departureId)
		TransitDriverStartsEvent ae = new TransitDriverStartsEvent(aeTs, new IdImpl(aeN), veh1, null, null, null);
		TransitDriverStartsEvent noD = new TransitDriverStartsEvent(noDTs, new IdImpl(noDN), veh1, null, null, null);
		TransitDriverStartsEvent negD = new TransitDriverStartsEvent(negDTs, new IdImpl(negDN), veh1, null, null, null);
		TransitDriverStartsEvent nor = new TransitDriverStartsEvent(norTs, new IdImpl(norN), veh1, null, null, null);
		//no TransitDriverStartsEvent for 'bycar' 
		TransitDriverStartsEvent difLinks1 = new TransitDriverStartsEvent(difLinks1Ts, new IdImpl(difLinks1N), veh1, null, null, null);
		TransitDriverStartsEvent difLinks2 = new TransitDriverStartsEvent(difLinks2Ts, new IdImpl(difLinks2N), veh1, null, null, null);
		TransitDriverStartsEvent difPer1 = new TransitDriverStartsEvent(difPer1Ts, new IdImpl(difPer1N), veh1, null, null, null);
		TransitDriverStartsEvent difPer2 = new TransitDriverStartsEvent(difPer2Ts, new IdImpl(difPer2N), veh1, null, null, null);
		
		//add events to list
		transitEvents.add(ae); transitEvents.add(noD);
		//TODO negative duration - see above
		//transitEvents.add(negD);
		transitEvents.add(nor); transitEvents.add(difLinks1); transitEvents.add(difLinks2); 
		transitEvents.add(difPer1); transitEvents.add(difPer2); 
		
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
	/*
 * AgentArrivalEvent aae = new AgentArrivalEvent(360, new IdImpl("50"), new IdImpl("13"), pt);
		//LinkLeaveEvent lle = new LinkLeaveEvent(70, new IdImpl("50"), new IdImpl("13"), new IdImpl("vehicle1"));
		//needed to test for vehicle hours
		AgentDepartureEvent ade = new AgentDepartureEvent(00, new IdImpl("50"), new IdImpl("13"), pt);
		
 * 
 * */
}

