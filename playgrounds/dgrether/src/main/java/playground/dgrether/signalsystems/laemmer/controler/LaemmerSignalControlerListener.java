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
package playground.dgrether.signalsystems.laemmer.controler;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.laemmer.model.LaemmerSignalModelFactory;


/**
 * @author dgrether
 *
 */
public class LaemmerSignalControlerListener implements SignalsControllerListener , StartupListener, IterationStartsListener,
		ShutdownListener {

	private SignalSystemsManager signalManager;
	private DgSensorManager sensorManager;

	
	@Override
	public void notifyStartup(StartupEvent event) {
		ScenarioImpl scenario = (ScenarioImpl) event.getControler().getScenario();
		
		this.sensorManager = new DgSensorManager(event.getControler().getScenario().getNetwork());
		this.sensorManager.setLaneDefinitions((LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		event.getControler().getEvents().addHandler(sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, 
				new LaemmerSignalModelFactory(new DefaultSignalModelFactory(), sensorManager) , event.getControler().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		SignalEngine engine = new QSimSignalEngine(this.signalManager);
		event.getControler().getMobsimListeners().add(engine);
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
