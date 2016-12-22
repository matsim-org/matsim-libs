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

import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.builder.SignalSystemsModelBuilder;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Provides;

/**
 * Add this module if you want to simulate fixed-time signals. 
 * It also works without signals.
 * 
 * @author tthunig
 */
public class SignalsModule extends AbstractModule {
	@Override
	public void install() {
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			// bindings for fixed-time signals
			bind(SignalModelFactory.class).to(DefaultSignalModelFactory.class);
			bind(SignalControlerListener.class).to(FixedTimeSignalControlerListener.class);

			// general signal bindings
			bind(SignalSystemsModelBuilder.class).to(FromDataBuilder.class);
			bind(QSimSignalEngine.class);
			addMobsimListenerBinding().to(QSimSignalEngine.class);

			// bind tool to write information about signal states for via
			bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
			addEventHandlerBinding().to(SignalEvents2ViaCSVWriter.class);
			addControlerListenerBinding().to(SignalEvents2ViaCSVWriter.class);
		}
	}

	@Provides
	SignalSystemsManager provideSignalSystemsManager(ReplanningContext replanningContext, FromDataBuilder modelBuilder) {
		SignalSystemsManager signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
		signalSystemsManager.resetModel(replanningContext.getIteration());
		return signalSystemsManager;
	}
}
