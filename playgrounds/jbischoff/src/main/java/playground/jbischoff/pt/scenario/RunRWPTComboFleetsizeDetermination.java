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
import org.matsim.contrib.av.flow.AvIncreasedCapacityModule;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class RunRWPTComboFleetsizeDetermination {
	public static void main(String[] args) {
	
		if (args.length!=4){
			throw new RuntimeException("Wrong arguments");
		}
		String configfile = args[0];
		String RUNID = args[1];
		String taxisFile = args[2];
		int avfactor = Integer.parseInt(args[3]);
		
		Config config = ConfigUtils.loadConfig(configfile, new TaxiConfigGroup(), new DvrpConfigGroup());
		config.controler().setRunId(RUNID);
		String outPutDir = config.controler().getOutputDirectory()+"/"+RUNID+"/"; 
		config.controler().setOutputDirectory(outPutDir);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		DvrpConfigGroup.get(config).setMode("taxi");
		
		TaxiConfigGroup tcg = (TaxiConfigGroup) config.getModules().get(TaxiConfigGroup.GROUP_NAME);
		tcg.setTaxisFile(taxisFile);
		
		VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
		
		
		walk.setDistance(500);
		walk.setTeleported(true);
		walk.setMode("walk");
		
		config.global().setNumberOfThreads(4);

		
		VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
		taxi.setDistance(200000);
		taxi.setTeleported(false);
		taxi.setMode("taxi");
		
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		vacfg.setAccessModeGroup(taxi);
		vacfg.setAccessModeGroup(walk);
		
		config.addModule(vacfg);
		config.transitRouter().setSearchRadius(5000);
		config.transitRouter().setExtensionRadius(0);
		
	   TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
	   taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
	   taxiCfg.setChangeStartLinkToLastLinkInSchedule(true);
       config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
       config.checkConsistency();

       Scenario scenario = ScenarioUtils.loadScenario(config);
       Controler controler = new Controler(scenario);
       controler.addOverridingModule(new AvIncreasedCapacityModule(avfactor));
       controler.addOverridingModule(new TaxiOutputModule());

       controler.addOverridingModule(new TaxiModule());

       controler.addOverridingModule(new VariableAccessTransitRouterModule());
//       controler.addOverridingModule(new TripHistogramModule());
//       controler.addOverridingModule(new OTFVisLiveModule());

       controler.run();
	}
}
