/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.randomizerPtRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;

import javax.inject.Provider;

public class RndPtRouterLauncher2 {

	
	public static void main(final String[] args) {
		String configFile ;
		String srtStopZoneConversion;
		if(args.length==0){
			configFile = "../../";
			srtStopZoneConversion = "";
		}else{
			configFile = args[0];
			srtStopZoneConversion = "false";
		}
		
		//load data
		Config config = ConfigUtils.loadConfig(configFile) ;
		final Scenario scn = ScenarioUtils.loadScenario(config);
		boolean isStopZoneConversion = Boolean.parseBoolean(srtStopZoneConversion);
		
		//set the controler
		final Controler controler = new Controler(scn);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		//create and set the factory for rndizedRouter 
		final TransitSchedule routerSchedule = scn.getTransitSchedule();
		final TransitRouterConfig trConfig = new TransitRouterConfig( config ) ;
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(routerSchedule, trConfig.beelineWalkConnectionDistance);
		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(routerSchedule);
		final Provider<TransitRouter> rndTrRouterFactory = new RndPtRouterFactory().createFactory (preparedSchedule, trConfig, routerNetwork, true, true);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(rndTrRouterFactory);
			}
		});

		//add analyzer for specific bus line and stop Zone conversion
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(isStopZoneConversion); 
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);  
	
		controler.run();
	}

}
