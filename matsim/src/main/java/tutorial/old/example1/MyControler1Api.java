/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

package tutorial.old.example1;


/**
 * This is an attempt to move this tutorial to the API.  We are not there yet, however.
 * 
 * @author nagel
 *
 */
public class MyControler1Api {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

//		ScenarioImpl scenario = new ScenarioImpl();
//		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFilename);
//
//		new MatsimPopulationReader(scenario).readFile(plansFilename);
//
//		EventsImpl events = new EventsImpl();
//
//		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
//		events.addHandler(eventWriter);
//
//		QueueSimulation sim = new QueueSimulation(scenario, events);
//		sim.run();
//
//		eventWriter.closeFile();

//		Scenario sc = (new ScenarioFactory()).createScenario() ;
//
//		Config config = sc.getConfig() ;
//		config.network().setInputFile(netFilename) ;
//		config.plans().setInputFile(plansFilename) ;
//		
//		ScenarioLoaderI scl = (new ScenarioLoaderFactory()).createScenarioLoader( sc ) ;
//		scl.loadScenario();
//		
//		EventsImpl events = new EventsImpl();
//
//		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
//		events.addHandler(eventWriter);
//
//		QueueSimulation sim = new QueueSimulation( (ScenarioImpl) sc, events);
//		sim.run();
//
//		eventWriter.closeFile();

	}

}
