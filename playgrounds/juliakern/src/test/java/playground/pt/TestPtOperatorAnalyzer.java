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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdHandler;
import playground.vsp.analysis.modules.ptOperator.PtOperatorAnalyzer;
import playground.vsp.analysis.modules.ptOperator.TransitEventHandler;
import playground.vsp.emissions.events.*;
import playground.vsp.emissions.types.ColdPollutant;

//test for playground.vsp.analysis.modules.ptOperator

public class TestPtOperatorAnalyzer {
	
	private PtDriverIdAnalyzer ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
	@Rule public MatsimTestUtils utils= new MatsimTestUtils();	

	//TODO keine Simulation starten
	//Variablennamen vs Werte
	//init implizit testen
	
@Test @Ignore
public final void testGetEventHandler(){
	/*
	//String netFilename = utils.getInputDirectory() + "network.xml"; //stimmt
	String plansFilename= utils.getInputDirectory() + "plans100.xml";
//	String netFilename = "test/scenarios/equil/network.xml";
//	String plansFilename = "test/scenarios/equil/plans100.xml";
	String runId = "testRun23";
	ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Config config = sc.getConfig();
	
	//config.network().setInputFile(netFilename);
	NetworkImpl network = (NetworkImpl) sc.getNetwork();
	Node node1 = network.createAndAddNode(sc.createId("1"), sc.createCoord(-20000.0,     0.0));
	Node node2 = network.createAndAddNode(sc.createId("2"), sc.createCoord(-17500.0,     0.0));
	network.createAndAddLink(sc.createId("1"), node1, node2, 1000, 27.78, 3600, 1, null, "22");
	network.createAndAddLink(sc.createId("2"), node1, node2, 1000, 27.78, 3600, 1, null, "23");

	config.plans().setInputFile(plansFilename);
	config.controler().setOutputDirectory(utils.getOutputDirectory());
	config.controler().setLastIteration(0);
	config.controler().setRunId(runId);
	Controler controler = new Controler(sc); //notwendig, um Logger zu benutzen... alternativ: sysos oder selbst logger init.
	controler.run();

	ptDriverIdAnalyzer.init(null);
	ptDriverIdAnalyzer.init(sc); //init is not part of the abstract analysis module //TODO set link?
	//therefore it is tested here
	
	TransitEventHandler teh = new TransitEventHandler(network, ptDriverIdAnalyzer);
	PtOperatorAnalyzer ptOpAna = new PtOperatorAnalyzer();
	LinkedList<EventHandler> handlerList = new LinkedList<EventHandler>();
	handlerList.add(teh);
	sc.addScenarioElement(teh);
	ptOpAna.init(sc);
	List<EventHandler> tore = ptOpAna.getEventHandler();
	System.out.println(tore);
	ptOpAna.init(sc);ptOpAna.init(sc);ptOpAna.init(sc);
	tore= ptOpAna.getEventHandler();
	tore= ptOpAna.getEventHandler();
	System.out.println(tore);*/
}

@Test @Ignore

public final void testWriteResults(){
	
}

//(so far) no need to test init
//TODO do i need to test preprocess? postprocess?

}

