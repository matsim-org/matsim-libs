/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxi.usability;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import playground.michalm.taxi.data.ETaxiData;
import playground.michalm.taxi.data.file.*;
import playground.michalm.taxi.ev.ETaxiUtils;

/**
 * @author jbischoff
 *
 */
public class ConfigBasedTaxiLaunchUtils {
		private MatsimVrpContextImpl context;
		private Controler controler;
		
		
		
		public ConfigBasedTaxiLaunchUtils(Controler controler) {
			this.controler = controler;
		}
		
	 
	public  void initiateTaxis(){
		//this is done exactly once per simulation
		final TaxiConfigGroup tcg = (TaxiConfigGroup) controler.getScenario().getConfig().getModule("taxiConfig");
      	context = new MatsimVrpContextImpl();
		context.setScenario(controler.getScenario());
        ETaxiData vrpData = new ETaxiData();
        new ETaxiReader(context.getScenario(), vrpData).parse(tcg.getVehiclesFile());
        new TaxiRankReader(context.getScenario(), vrpData).parse(tcg.getRanksFile());
		context.setVrpData(vrpData);	 
		TaxiStatsControlerListener tscl = new TaxiStatsControlerListener(context,tcg);
		controler.addControlerListener(tscl);
		controler.addOverridingModule(new AbstractModule() {
			
						
			@Override
			public void install() {
				addRoutingModuleBinding("taxi").toInstance(new TaxiserviceRoutingModule(controler));
				
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				bindMobsim().toProvider(TaxiQSimProvider.class);
				bind(MatsimVrpContext.class).toInstance(context);
			}
		});
		
		
		
	} 
}
