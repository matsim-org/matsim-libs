/* *********************************************************************** *
 * project: kai
 * KaiControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package tutorial.programming.simpleadaptivesignalengine;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Provider;

public class RunSimpleAdaptiveSignalExample {

	public static void main(String[] args) {
		final boolean useOTFVis = false ;
		
		final Controler controler = new Controler( "examples/config/daganzo-config.xml" ) ;

		final SimpleAdaptiveSignal simpleAdaptiveSignalEngine = new SimpleAdaptiveSignal(controler) ;

		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( new Provider<Mobsim>() {
					@Inject Scenario sc ;
					@Inject EventsManager events ;

					@Override
					public Mobsim get() {
						QSim qsim = QSimUtils.createDefaultQSim(sc, events);
						qsim.addQueueSimulationListeners(simpleAdaptiveSignalEngine) ;
						events.addHandler(simpleAdaptiveSignalEngine) ;
//						if ( useOTFVis ) {
//							OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, events, qsim);
//							OTFClientLive.run(sc.getConfig(), server);
//						}
						return qsim ;
					}
				} ) ;
			}
		});
//		if ( useOTFVis ) {
//			ConfigUtils.addOrGetModule(controler.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setColoringScheme( ColoringScheme.byId ) ;
//		}
		controler.run();
	
	}

}
