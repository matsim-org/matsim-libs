/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SignalsModule.java
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

package org.matsim.contrib.signals.controler;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.router.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.replanning.ReplanningContext;

public class SignalsModule extends AbstractModule {
    @Override
    public void install() {
        if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
            bind(SignalsControllerListener.class).to(DefaultSignalsControllerListener.class);
            addControlerListenerBinding().to(SignalsControllerListener.class);
            addMobsimListenerBinding().to(QSimSignalEngine.class);
            // bind tool to write information about signal states for via
			bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
            addEventHandlerBinding().to(SignalEvents2ViaCSVWriter.class);
            addControlerListenerBinding().to(SignalEvents2ViaCSVWriter.class);

            if (getConfig().controler().isLinkToLinkRoutingEnabled()){
                //use the extended NetworkWithSignalsTurnInfoBuilder (instead of NetworkTurnInfoBuilder)
                //michalm, jan'17
                bind(NetworkTurnInfoBuilder.class).to(NetworkWithSignalsTurnInfoBuilder.class);
            }
        }
    }

    @Provides SignalSystemsManager provideSignalSystemsManager(Scenario scenario, EventsManager eventsManager, ReplanningContext replanningContext) {
        FromDataBuilder modelBuilder = new FromDataBuilder(scenario, eventsManager);
        SignalSystemsManager signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
        signalSystemsManager.resetModel(replanningContext.getIteration());
        return signalSystemsManager;
    }

    @Provides QSimSignalEngine provideQSimSignalEngine(SignalSystemsManager signalManager) {
        return new QSimSignalEngine(signalManager);
    }
}
