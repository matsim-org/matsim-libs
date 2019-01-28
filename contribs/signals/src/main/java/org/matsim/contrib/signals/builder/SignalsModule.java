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

package org.matsim.contrib.signals.builder;

import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.controller.sylvia.SylviaSignalController;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.sensor.DownstreamSensor;
import org.matsim.contrib.signals.sensor.LinkSensorManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSignalsNetworkFactory;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;

import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

/**
 * Add this module if you want to simulate signals. It does not work without
 * signals. By default, it works with without signals and with signal
 * implementations of fixed-time signals, traffic-actuated signals called SYLVIA
 * and traffic-adaptive signals based on Laemmer. If you want to add other
 * signal controllers, you can add a respective factory by calling the method
 * addSignalControllerFactory. It is also possible to use different control
 * schemes in one scenario at different intersections (i.e. signal systems).
 * 
 * @author tthunig
 */
class SignalsModule extends AbstractModule {
	// This is no longer public since there is now also material that needs to be injected at the QSim level (see
	// Signals.configure(...)), and making SignalsModule nonpublic seems the best way of forcibly notifying users.  kai, nov'18

	private MapBinder<String, SignalControllerFactory> signalControllerFactoryMultibinder;
	private Map<String, Class<? extends SignalControllerFactory>> signalControllerFactoryClassNames = new HashMap<>();

	SignalsModule() {
		// specify default signal controller. you can add your own by calling addSignalControllerFactory (see method java-doc below)
		signalControllerFactoryClassNames.put(DefaultPlanbasedSignalSystemController.IDENTIFIER, DefaultPlanbasedSignalSystemController.FixedTimeFactory.class);
		signalControllerFactoryClassNames.put(SylviaSignalController.IDENTIFIER, SylviaSignalController.SylviaFactory.class);
		signalControllerFactoryClassNames.put(LaemmerSignalController.IDENTIFIER, LaemmerSignalController.LaemmerFactory.class);
	}
	
	@Override
	public void install() {
		this.signalControllerFactoryMultibinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<SignalControllerFactory>() {});
		
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			// bindings for sensor-based signals (also works for fixed-time signals)
			bind(SignalModelFactory.class).to(SignalModelFactoryImpl.class);
			addControlerListenerBinding().to(SensorBasedSignalControlerListener.class);
			bind(LinkSensorManager.class).asEagerSingleton();
			bind(DownstreamSensor.class).asEagerSingleton();
//			// bind factory for all specified signal controller
			for (String identifier : signalControllerFactoryClassNames.keySet()) {
				/* note: This cannot be called before (e.g. in the constructor or from outside),
				 * as the binder in AbstractModule that is needed here is only created in method
				 * configure()... theresa, aug'18 */
				signalControllerFactoryMultibinder.addBinding(identifier).to(signalControllerFactoryClassNames.get(identifier));
			}
			
			// general signal bindings
			bind(SignalSystemsManager.class).toProvider(FromDataBuilder.class);
			addMobsimListenerBinding().to(QSimSignalEngine.class);
//			bind(QNetworkFactory.class).to(QSignalsNetworkFactory.class);

			// bind tool to write information about signal states for via
			bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
			addControlerListenerBinding().to(SignalEvents2ViaCSVWriter.class);
			addEventHandlerBinding().to(SignalEvents2ViaCSVWriter.class);

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
	
	/**
	 * Call this method when you want to add your own SignalController. E.g. via signalsModule.addSignalControllerFactory().to(LaemmerSignalController.LaemmerFactory.class)
	 * 
	 * @param signalControllerFactoryClassName
	 */
	final void addSignalControllerFactory(String key, Class<? extends SignalControllerFactory> signalControllerFactoryClassName) {
		this.signalControllerFactoryClassNames.put(key, signalControllerFactoryClassName);
	}
}
