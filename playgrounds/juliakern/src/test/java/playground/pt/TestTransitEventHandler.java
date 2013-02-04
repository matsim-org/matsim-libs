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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.sun.xml.bind.v2.schemagen.xmlschema.List;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdHandler;
import playground.vsp.analysis.modules.ptOperator.TransitEventHandler;


//test for playground.vsp.analysis.modules.ptOperator

public class TestTransitEventHandler {

	private Network network;
	private PtDriverIdAnalyzer ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
	private PtDriverIdHandler ptDriverHandler= new PtDriverIdHandler();
	@Rule public MatsimTestUtils utils= new MatsimTestUtils();
	@Test @Ignore
	public final void testHandleEventAgentArrival(){
	
		String netFilename = utils.getInputDirectory() + "network.xml"; //stimmt
		String plansFilename= utils.getInputDirectory() + "plans100.xml";
//		String netFilename = "test/scenarios/equil/network.xml";
//		String plansFilename = "test/scenarios/equil/plans100.xml";
		String runId = "testRun23";
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = sc.getConfig();
		
		config.network().setInputFile(netFilename);
		NetworkImpl network = (NetworkImpl) sc.getNetwork();
		Node node1 = network.createAndAddNode(sc.createId("1"), sc.createCoord(-20000.0,     0.0));
		Node node2 = network.createAndAddNode(sc.createId("2"), sc.createCoord(-17500.0,     0.0));
		network.createAndAddLink(sc.createId("1"), node1, node2, 1000, 27.78, 3600, 1, null, "22");

		System.out.println(config.network().toString());
		config.plans().setInputFile(plansFilename);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setRunId(runId);
		Controler controler = new Controler(sc);
		controler.run();
		
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
		
		//das Netzwerk ist leer bzw enthaelt keine knoten. warum?
		//network=sc.getNetwork();
		
		ptDriverIdAnalyzer.init(sc);
		
		TransitEventHandler teh = new TransitEventHandler(network, ptDriverIdAnalyzer);
		teh.reset(0);		
			for(TransitDriverStartsEvent e: transitEvents){
			pdih.handleEvent(e);
		}
		
		for(AgentDepartureEvent e: departureEvents){
			teh.handleEvent(e);
		}
		
		for(AgentArrivalEvent e: arrivalEvents){
			teh.handleEvent(e);
		}
		
		Double exp= 360.-00.+100.-100.+3620.-20; //+ 0 for 'bycar'
		exp = exp + 900.-540.;// + 900.-540.;
		exp= exp/3600.0;
		Assert.assertFalse("by car is not public transport", ptDriverIdAnalyzer.isPtDriver(new IdImpl("by car")));
		LinkedList<String> ptList = new LinkedList<String>();
		ptList.add("50");ptList.add("zero duration");//ptList.add("negative duration");
		ptList.add("normal");
		ptList.add("different links"); //ptList.add("different persons"); ptList.add("different persons2");
		for(String id: ptList ){
			if(ptDriverIdAnalyzer.isPtDriver(new IdImpl(id))==false)System.out.println(id+ "is not ");
			Assert.assertTrue("public transport", ptDriverIdAnalyzer.isPtDriver(new IdImpl(id)));
		}
		
		Assert.assertEquals("total duration should be"+exp, exp, teh.getVehicleHours(), MatsimTestUtils.EPSILON);
	}

	private void createArrivalEvents(LinkedList<AgentArrivalEvent> arrivalEvents) {
		IdImpl link1 = new IdImpl("1"); IdImpl link2 = new IdImpl("2");
		
		AgentArrivalEvent aae = new AgentArrivalEvent(360, new IdImpl("50"), new IdImpl("13"), "pt");
		AgentArrivalEvent noD = new AgentArrivalEvent(100, new IdImpl("zero duration"), link1, "pt");
		AgentArrivalEvent negD = new AgentArrivalEvent(250, new IdImpl("negative duration"), link1, "pt");
		AgentArrivalEvent nor = new AgentArrivalEvent(3620, new IdImpl("normal"), link1, "pt");
		AgentArrivalEvent bycar = new AgentArrivalEvent(100, new IdImpl("by car"), link1, "car");
		AgentArrivalEvent difLinks2 = new AgentArrivalEvent(900, new IdImpl("different links"), link2, "pt");
		AgentArrivalEvent difLinks1 = new AgentArrivalEvent(900, new IdImpl("different links"), link1, "pt");
		AgentArrivalEvent diffPer1 = new AgentArrivalEvent(450, new IdImpl("different persons"), link1, "pt");
		AgentArrivalEvent diffPer2 = new AgentArrivalEvent(450, new IdImpl("different persons2"), link1, "pt");
		
		arrivalEvents.add(aae); arrivalEvents.add(noD);//arrivalEvents.add(negD);
		arrivalEvents.add(nor);//arrivalEvents.add(bycar);
		arrivalEvents.add(difLinks2);
		//arrivalEvents.add(difLinks1);arrivalEvents.add(diffPer1);arrivalEvents.add(diffPer2);
		
	}
	private void createDepartureEvents(LinkedList<AgentDepartureEvent> departureEvents) {
		IdImpl link1 = new IdImpl("1"); IdImpl link2 = new IdImpl("2"); 
		
		AgentDepartureEvent ade = new AgentDepartureEvent(00, new IdImpl("50"), new IdImpl("13"), "pt");
		AgentDepartureEvent noD = new AgentDepartureEvent(100, new IdImpl("zero duration"), link1, "pt");
		AgentDepartureEvent negD = new AgentDepartureEvent(5000, new IdImpl("negative duration"), link1, "pt");
		AgentDepartureEvent nor = new AgentDepartureEvent(20, new IdImpl("normal"), link1, "pt");
		AgentDepartureEvent bycar = new AgentDepartureEvent(1180, new IdImpl("by car"), link1, "car");
		AgentDepartureEvent difLinks1 = new AgentDepartureEvent(540, new IdImpl("different links"), link1, "pt");
		AgentDepartureEvent difLinks2 = new AgentDepartureEvent(540, new IdImpl("different links"), link2, "pt");
		AgentDepartureEvent diffPer1 = new AgentDepartureEvent(126, new IdImpl("different persons"), link1, "pt");
		AgentDepartureEvent diffPer2 = new AgentDepartureEvent(126, new IdImpl("different persons2"), link1, "pt");
		departureEvents.add(ade); departureEvents.add(noD); departureEvents.add(negD);
		departureEvents.add(nor);departureEvents.add(bycar);departureEvents.add(difLinks1);
		departureEvents.add(difLinks2);departureEvents.add(diffPer1);departureEvents.add(diffPer2);
		
	}	
	private void createTransitEvents(LinkedList<TransitDriverStartsEvent> transitEvents) {
		IdImpl veh1 = new IdImpl("vehicle 1");
		//TODO wann bzgl der departure events muessen diese stattfinden?
		TransitDriverStartsEvent transitEvent = new TransitDriverStartsEvent(20, new IdImpl("50"), veh1, null, null, null);
		TransitDriverStartsEvent noD = new TransitDriverStartsEvent(100, new IdImpl("zero duration"), veh1, null, null, null);
		TransitDriverStartsEvent negD = new TransitDriverStartsEvent(0, new IdImpl("negative duration"), veh1, null, null, null);
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
		transitEvents.add(difLinks2); //transitEvents.add(diffPer1); transitEvents.add(diffPer2); 
		
	}
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
	
	//TODO do i need to test the reset?
	//(so far) no need to test getVehicleIds, getVehicleKm
	/*
 * AgentArrivalEvent aae = new AgentArrivalEvent(360, new IdImpl("50"), new IdImpl("13"), "pt");
		//LinkLeaveEvent lle = new LinkLeaveEvent(70, new IdImpl("50"), new IdImpl("13"), new IdImpl("vehicle1"));
		//needed to test for vehicle hours
		AgentDepartureEvent ade = new AgentDepartureEvent(00, new IdImpl("50"), new IdImpl("13"), "pt");
		
 * 
 * */
}

