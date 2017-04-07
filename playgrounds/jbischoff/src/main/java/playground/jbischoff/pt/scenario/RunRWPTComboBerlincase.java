/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.pt.strategy.ChangeSingleLegModeWithPredefinedFromModesModule;

/**
 * @author  jbischoff
 *
 */
public class RunRWPTComboBerlincase {
	
	public static void main(String[] args) {
		if (args.length!=1){
			throw new RuntimeException("Wrong arguments");
		}
		String configfile = args[0];
		
		Config config = ConfigUtils.loadConfig(configfile, new TaxiConfigGroup(), new DvrpConfigGroup(), new  VariableAccessConfigGroup(), new TaxiFareConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		DvrpConfigGroup.get(config).setMode(TaxiModule.TAXI_MODE);

       config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
       config.checkConsistency();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TaxiOutputModule());
		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		controler.addOverridingModule(new ChangeSingleLegModeWithPredefinedFromModesModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		controler.run();
	}
}
