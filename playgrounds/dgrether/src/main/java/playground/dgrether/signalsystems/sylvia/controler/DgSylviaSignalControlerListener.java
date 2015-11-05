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
package playground.dgrether.signalsystems.sylvia.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.model.DgSylviaSignalModelFactory;


/**
 * @author dgrether
 *
 */
public class DgSylviaSignalControlerListener implements SignalsControllerListener , StartupListener, IterationStartsListener,
		ShutdownListener {

	private SignalSystemsManager signalManager;
	private DgSensorManager sensorManager;
	private DgSylviaConfig sylviaConfig;
	private boolean alwaysSameMobsimSeed = false ;
	
	public DgSylviaSignalControlerListener(DgSylviaConfig sylviaConfig) {
		this.sylviaConfig = sylviaConfig;
	}

	public DgSylviaSignalControlerListener(DgSylviaConfig sylviaConfig, boolean alwaysSameMobsimSeed ) {
		this.sylviaConfig = sylviaConfig;
		this.alwaysSameMobsimSeed = alwaysSameMobsimSeed ;
	}


	@Override
	public void notifyStartup(StartupEvent event) {
		MutableScenario scenario = (MutableScenario) event.getControler().getScenario();
		
		this.sensorManager = new DgSensorManager(event.getControler().getScenario().getNetwork());
		if ( scenario.getConfig().network().getLaneDefinitionsFile()!=null || scenario.getConfig().qsim().isUseLanes()){
			this.sensorManager.setLaneDefinitions((Lanes) scenario.getScenarioElement(Lanes.ELEMENT_NAME));
		}
		event.getControler().getEvents().addHandler(sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, 
				new DgSylviaSignalModelFactory(new DefaultSignalModelFactory(), sensorManager, this.sylviaConfig) , event.getControler().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		SignalEngine engine = new QSimSignalEngine(this.signalManager);
		event.getControler().getMobsimListeners().add(engine);
		
		if ( this.alwaysSameMobsimSeed ) {
			MobsimInitializedListener randomSeedResetter = new MobsimInitializedListener(){
				@Override
				public void notifyMobsimInitialized(MobsimInitializedEvent e) {
					MatsimRandom.reset(0); // make sure it is same random seed every time
				}
			};
			event.getControler().getMobsimListeners().add(randomSeedResetter) ;
		}
	}


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
		this.sensorManager.reset(event.getIteration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.writeData(event.getControler().getScenario(), event.getControler().getControlerIO());
	}
	
	public void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
		SignalsData data = (SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME);
		new SignalsScenarioWriter(controlerIO).writeSignalsData(data);
	}
	
	
}
