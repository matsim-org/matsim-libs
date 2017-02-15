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
package playground.kai.usecases.test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

public class MyADClass {
 
       public static void main(String[] args) {
      	 
      	 Config config = ConfigUtils.loadConfig("input/config.xml");
       
      	 Scenario scenario = ScenarioUtils.loadScenario( config ) ;
       
      	 Controler controler = new Controler( scenario ) ;
      	 
      	 controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.addMobsimListenerBinding().toInstance(new MobsimBeforeSimStepListener(){
					@Override public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
						for (  MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents().values() ) {
						}
					}
				});
			}
      	 });
      	 
      	 controler.run() ;
      	 
       }
       
}