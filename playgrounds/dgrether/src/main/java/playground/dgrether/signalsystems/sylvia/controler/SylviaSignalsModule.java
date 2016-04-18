/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package playground.dgrether.signalsystems.sylvia.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Provides;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.model.DgSylviaSignalModelFactory;

/**
 * Add this module if you want to simulate signals with sylvia. 
 * It also works with fixed time signals, i.e. without sylvia (with the default controller identifier in the signal control file) 
 * and without signals.
 * 
 * @author tthunig
 */
public class SylviaSignalsModule extends AbstractModule {

	private boolean alwaysSameMobsimSeed = false;
	private DgSylviaConfig sylviaConfig = new DgSylviaConfig();
	
	@Override
	public void install() {
		if (alwaysSameMobsimSeed) {
			MobsimInitializedListener randomSeedResetter = new MobsimInitializedListener() {
				@Override
				public void notifyMobsimInitialized(MobsimInitializedEvent e) {
					// make sure it is same random seed every time
					MatsimRandom.reset(0);
				}
			};
			this.addMobsimListenerBinding().toInstance(randomSeedResetter);
		}
		
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
            bind(SignalsControllerListener.class).to(DgSylviaSignalControlerListener.class);
            addControlerListenerBinding().to(SignalsControllerListener.class);
            addMobsimListenerBinding().to(QSimSignalEngine.class);
            addEventHandlerBinding().to(DgSensorManager.class);
        }
	}
	
	@Provides DgSensorManager provideDgSensorManager(Scenario scenario) {
		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		if (scenario.getConfig().network().getLaneDefinitionsFile() != null || scenario.getConfig().qsim().isUseLanes()) {
			sensorManager.setLaneDefinitions(scenario.getLanes());
		}
		return sensorManager;
	}
	
	@Provides SignalSystemsManager provideSignalSystemsManager(Scenario scenario, EventsManager eventsManager, ReplanningContext replanningContext, DgSensorManager sensorManager) {	
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, new DgSylviaSignalModelFactory(new DefaultSignalModelFactory(), sensorManager, sylviaConfig), eventsManager);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		signalManager.resetModel(replanningContext.getIteration());		
		return signalManager;
    }
	
	@Provides QSimSignalEngine provideQSimSignalEngine(SignalSystemsManager signalManager) {
        return new QSimSignalEngine(signalManager);
    }

	public void setAlwaysSameMobsimSeed(boolean alwaysSameMobsimSeed) {
		this.alwaysSameMobsimSeed = alwaysSameMobsimSeed;
	}

	public void setSylviaConfig(DgSylviaConfig sylviaConfig) {
		this.sylviaConfig = sylviaConfig;
	}

}
