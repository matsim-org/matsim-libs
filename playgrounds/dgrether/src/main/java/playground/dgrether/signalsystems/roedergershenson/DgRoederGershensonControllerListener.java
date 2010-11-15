/* *********************************************************************** *
 * project: org.matsim.*
 * DgRoederGershensonControllerListener
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
package playground.dgrether.signalsystems.roedergershenson;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.DefaultSignalModelFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.initialization.SignalsControllerListener;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.dgrether.signalsystems.DgSensorManager;


/**
 * @author dgrether
 *
 */
public class DgRoederGershensonControllerListener implements SignalsControllerListener, StartupListener, ShutdownListener, IterationStartsListener {
	
	private SignalSystemsManager signalManager;
	private SignalsData signalsData;
	private QSimSignalEngine signalEngie;
	
	public DgRoederGershensonControllerListener() {
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		ScenarioImpl scenario = event.getControler().getScenario();
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(signalsData, new DgGershensonRoederSignalModelFactory(new DefaultSignalModelFactory()) , event.getControler().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();

		
		//TODO init gershenson controller and sensor manager here
		DgSensorManager sensorManager = new DgSensorManager();
		event.getControler().getEvents().addHandler(sensorManager);
		for (SignalSystem ss : this.signalManager.getSignalSystems().values()){
			if (ss.getSignalController() instanceof DgRoederGershensonController){
				((DgRoederGershensonController)ss.getSignalController()).initSignalGroupMetadata(scenario.getNetwork(), scenario.getLaneDefinitions());
				((DgRoederGershensonController)ss.getSignalController()).registerAndInitializeSensorManager(sensorManager);
			
			}
		}
		
		
		
		this.signalEngie = new QSimSignalEngine(this.signalManager);
		event.getControler().getQueueSimulationListener().add(this.signalEngie);
	}

	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.writeData(event.getControler().getScenario(), event.getControler().getControlerIO().getOutputPath());
	}


	public void writeData(Scenario sc, String outputPath) {
		SignalsData data = sc.getScenarioElement(SignalsData.class);
		new SignalsScenarioWriter(outputPath).writeSignalsData(data);
	}

}
