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

import org.apache.log4j.Logger;
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
public class SignalsModule extends AbstractModule {
	public SignalsModule() {
		throw new RuntimeException("Some bindings have moved to a separate SignalsQSimModule.  " +
                "In order to notify the user of this change, the material from SignalsModule has been " +
                "moved to SignalsModuleV2.  You now need\n" +
                "   controler.addOverridingModule( new SignalsModuleV2() ) ;\n" +
                "   controler.addOverridingModule( new SignalsQSimModule() )\n" +
                "in your code.") ;
	}

    @Override
    public void install(){
        throw new RuntimeException( "not implemented" );
    }

}
