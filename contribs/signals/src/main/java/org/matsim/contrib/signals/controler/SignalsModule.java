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
import org.matsim.contrib.signals.router.NetworkWithSignalsTurnInfoBuilder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSignalsNetworkFactory;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Provides;

/**
 * Add this module if you want to simulate signals. It also works without
 * signals. By default, it works with signal implementations of fixed-time
 * signals, traffic-actuated signals called SYLVIA and traffic-adaptive signals
 * based on Laemmer. If you want to add other signal controllers, you can add a
 * respective provider by calling the method addSignalControlProvider. It is
 * also possible to use different control schemes in one scenario at different
 * intersections (i.e. signal systems).
 * 
 * @author tthunig
 */
public class SignalsModule extends AbstractModule {
	
	@Override
	public void install() {
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			// bindings for fixed-time signals
			bind(SignalModelFactory.class).to(DefaultSignalModelFactory.class);
			addControlerListenerBinding().to(DefaultSignalControlerListener.class);

			// general signal bindings
			bind(SignalSystemsModelBuilder.class).to(FromDataBuilder.class);
			addMobsimListenerBinding().to(QSimSignalEngine.class);
			bind(QNetworkFactory.class).to(QSignalsNetworkFactory.class);

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

	@Provides
	SignalSystemsManager provideSignalSystemsManager(ReplanningContext replanningContext, SignalSystemsModelBuilder modelBuilder) {
		SignalSystemsManager signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
		signalSystemsManager.resetModel(replanningContext.getIteration());
		return signalSystemsManager;
	}
}
