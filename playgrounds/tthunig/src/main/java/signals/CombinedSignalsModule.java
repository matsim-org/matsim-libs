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
package signals;

import com.google.inject.Singleton;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.builder.SignalSystemsModelBuilder;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.router.NetworkWithSignalsTurnInfoBuilder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Provides;

import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.downstreamSensor.DownstreamSensor;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.SignalAnalyzer;
import signals.sensor.LinkSensorManager;
import signals.sensor.SensorBasedSignalControlerListener;
import signals.sylvia.controler.DgSylviaConfig;

/**
 * Add this module if you want to simulate fixed-time signals, sylvia, laemmer, gershenson or the downstream signals or different control schemes together at different intersections (i.e. systems) in
 * your scenario. It also works without signals.
 * 
 * @author tthunig
 *
 */
public class CombinedSignalsModule extends AbstractModule {
	
	private boolean alwaysSameMobsimSeed = false;
	private DgSylviaConfig sylviaConfig = new DgSylviaConfig();
	private LaemmerConfig laemmerConfig = new LaemmerConfig();
	
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
			bind(DgSylviaConfig.class).toInstance(sylviaConfig);
			bind(LaemmerConfig.class).toInstance(laemmerConfig);
			bind(TtTotalDelay.class).asEagerSingleton();
			bind(SignalModelFactory.class).to(CombinedSignalModelFactory.class);
			addControlerListenerBinding().to(SensorBasedSignalControlerListener.class);
			bind(LinkSensorManager.class).asEagerSingleton();
			bind(DownstreamSensor.class).asEagerSingleton();
			bind(SignalAnalyzer.class);
			addMobsimListenerBinding().to(SignalAnalyzer.class);
			
			// general signal bindings
			bind(SignalSystemsModelBuilder.class).to(FromDataBuilder.class);
			addMobsimListenerBinding().to(QSimSignalEngine.class);

			// bind tool to write information about signal states for via
			bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
			/* asEagerSingleton is necessary to force creation of the SignalEvents2ViaCSVWriter class as it is never used somewhere else. theresa dec'16 */
			
			if (getConfig().qsim().isUsingFastCapacityUpdate()) {
                throw new RuntimeException("Fast flow capacity update does not support signals");
            }
		}
		if (getConfig().controler().isLinkToLinkRoutingEnabled()){
            //use the extended NetworkWithSignalsTurnInfoBuilder (instead of NetworkTurnInfoBuilder)
            //michalm, jan'17
            bind(NetworkTurnInfoBuilderI.class).to(NetworkWithSignalsTurnInfoBuilder.class);
        }        
	}

	@Singleton @Provides
	SignalSystemsManager provideSignalSystemsManager(ReplanningContext replanningContext, SignalSystemsModelBuilder modelBuilder) {
		SignalSystemsManager signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
		signalSystemsManager.resetModel(replanningContext.getIteration());
		return signalSystemsManager;
	}
	
	public void setAlwaysSameMobsimSeed(boolean alwaysSameMobsimSeed) {
		this.alwaysSameMobsimSeed = alwaysSameMobsimSeed;
	}

	public void setSylviaConfig(DgSylviaConfig sylviaConfig) {
		this.sylviaConfig = sylviaConfig;
	}

	public void setLaemmerConfig(LaemmerConfig laemmerConfig) {
		this.laemmerConfig = laemmerConfig;
	}
}
