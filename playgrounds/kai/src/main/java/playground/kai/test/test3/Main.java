/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.test.test3;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
class Main {

	public static class MyMobsimProvider implements Provider<Mobsim> {
		@Inject Scenario scenario ;
		@Inject EventsManager eventsManager ;
		@Inject MyClass2 class2 ;
		@Inject MyClass3 class3 ;
		@Override
		public Mobsim get() {
			Logger.getLogger(this.getClass()).warn( " class2=" + class2 );
			Logger.getLogger(this.getClass()).warn( " class3=" + class3 );

			Mobsim qsim = QSimUtils.createDefaultQSim(scenario, eventsManager) ;
			return qsim ;
		}
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bind(MyClass2.class ) ;
				this.bind(MyClass3.class) ;
				this.bind(MyClass1.class) ;
				
				this.bindMobsim().toProvider(MyMobsimProvider.class) ;
			}
		}); 

		controler.run() ;
		
	}

}
