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

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.lanes.data.Lanes;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemsManager;

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
		MutableScenario scenario = (MutableScenario) event.getServices().getScenario();
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, new DgGershensonRoederSignalModelFactory(new DefaultSignalModelFactory()) , event.getServices().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();

		
		//TODO init gershenson controller and sensor manager here
		DgSensorManager sensorManager = new DgSensorManager(scenario);
		event.getServices().getEvents().addHandler(sensorManager);
		for (SignalSystem ss : this.signalManager.getSignalSystems().values()){
			if (ss.getSignalController() instanceof DgRoederGershensonController){
				((DgRoederGershensonController)ss.getSignalController()).initSignalGroupMetadata(scenario.getNetwork(), (Lanes) scenario.getScenarioElement(Lanes.ELEMENT_NAME));
				((DgRoederGershensonController)ss.getSignalController()).registerAndInitializeSensorManager(sensorManager);
			
			}
		}
		
		
		
		this.signalEngie = new QSimSignalEngine(this.signalManager);
//		event.getServices().getMobsimListeners().add(this.signalEngie);
		throw new RuntimeException();
	}

	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		new SignalsScenarioWriter(event.getServices().getControlerIO()).writeSignalsData(event.getServices().getScenario());
	}



}
