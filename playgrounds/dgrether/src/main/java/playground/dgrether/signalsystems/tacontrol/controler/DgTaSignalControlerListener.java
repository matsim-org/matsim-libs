/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaSignalControlerListener
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.tacontrol.controler;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.signalsystems.builder.DefaultSignalModelFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.controler.SignalsControllerListener;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.tacontrol.model.DgTaSignalModelFactory;


/**
 * @author dgrether
 *
 */
public class DgTaSignalControlerListener implements SignalsControllerListener , StartupListener, IterationStartsListener,
		ShutdownListener {

	private SignalSystemsManager signalManager;
	private DgSensorManager sensorManager;

	
	@Override
	public void notifyStartup(StartupEvent event) {
		ScenarioImpl scenario = event.getControler().getScenario();
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		
		this.sensorManager = new DgSensorManager(event.getControler().getScenario().getNetwork());
		this.sensorManager.setLaneDefinitions(scenario.getLaneDefinitions());
		event.getControler().getEvents().addHandler(sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(signalsData, 
				new DgTaSignalModelFactory(new DefaultSignalModelFactory(), sensorManager) , event.getControler().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		SignalEngine engine = new QSimSignalEngine(this.signalManager);
		event.getControler().getQueueSimulationListener().add(engine);
	}


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
		this.sensorManager.reset(event.getIteration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

	}

	
	
}
