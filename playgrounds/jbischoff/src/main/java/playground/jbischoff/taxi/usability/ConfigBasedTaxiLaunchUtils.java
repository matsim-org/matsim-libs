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
import org.matsim.core.router.TripRouterFactory;

import playground.michalm.taxi.run.TaxiLauncherUtils;

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
		VrpData vrpData = TaxiLauncherUtils.initTaxiData(context.getScenario(), tcg.getVehiclesFile(), tcg.getRanksFile());
		context.setVrpData(vrpData);	 
		TaxiStatsControlerListener tscl = new TaxiStatsControlerListener(context,tcg);
		controler.addControlerListener(tscl);
        TripRouterFactory factory = new TaxiTripRouterFactory(controler); 
		controler.setTripRouterFactory(factory);
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				bindMobsim().toProvider(TaxiQSimProvider.class);
				bind(MatsimVrpContext.class).toInstance(context);
			}
		});
		
		
		
	} 
}
