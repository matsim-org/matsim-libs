package playground.dgrether;

import java.net.URL;
import java.net.URLClassLoader;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.dgrether.utils.DgOTFVisUtils;

/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * @author dgrether
 *
 */
public class DgOTFVis {

	
	public void playScenario(Scenario scenario) {
		EventsManagerImpl events = new EventsManagerImpl();
		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		QSim qSim = new QSim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(qSim);
		qSim.addFeature(queueSimulationFeature);
		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
		qSim.setControlerIO(controlerIO);
		qSim.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		qSim.run();

	}

	
	public void playAndRouteConfig(String config){
		Scenario sc = new ScenarioLoaderImpl(config).loadScenario();
		DgOTFVisUtils.locateAndRoutePopulation(sc);
		this.playScenario(sc);
	}
	
	
	public static void  printClasspath(){
		System.out.println("Classpath: ");
	//Get the System Classloader
    ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
    //Get the URLs
    URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
    for(int i=0; i< urls.length; i++)
    {
        System.out.println("  " + urls[i].getFile());
    }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


}
