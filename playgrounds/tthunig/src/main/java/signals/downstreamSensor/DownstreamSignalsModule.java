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
package signals.downstreamSensor;

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
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Provides;

import playground.dgrether.signalsystems.sensor.LinkSensorManager;
import playground.dgrether.signalsystems.sensor.SensorBasedSignalControlerListener;
import signals.CombinedSignalsModule;

/**
 * Add this module to the controler if you want to simulate downstream signals, i.e.
 * signals that switch to red if the downstream link is full
 * 
 * @deprecated use {@link CombinedSignalsModule} instead
 * 
 * @author tthunig
 *
 */
@Deprecated
public class DownstreamSignalsModule extends AbstractModule{

	@Override
	public void install() {
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			// signal specific bindings
			bind(SignalModelFactory.class).to(DownstreamSignalModelFactory.class);
			
			// bindings for sensor based signals
			bind(LinkSensorManager.class);
			addControlerListenerBinding().to(SensorBasedSignalControlerListener.class);
            bind(DownstreamSensor.class);
			
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
            bind(NetworkTurnInfoBuilder.class).to(NetworkWithSignalsTurnInfoBuilder.class);
        }
	}
	
	@Provides SignalSystemsManager provideSignalSystemsManager(ReplanningContext replanningContext, SignalSystemsModelBuilder modelBuilder) {	
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		signalManager.resetModel(replanningContext.getIteration());		
		return signalManager;
    }
	
}
