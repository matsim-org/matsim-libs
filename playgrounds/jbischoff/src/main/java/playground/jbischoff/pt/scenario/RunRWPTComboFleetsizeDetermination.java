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
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


import playground.jbischoff.pt.VariableAccessConfigGroup;
import playground.jbischoff.pt.VariableAccessModeConfigGroup;
import playground.jbischoff.pt.VariableAccessTransitRouterModule;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunRWPTComboFleetsizeDetermination {
public static void main(String[] args) {
	
		if (args.length!=3){
			throw new RuntimeException("Wrong arguments");
		}
		String configfile = args[0];
		String RUNID = args[1];
		String taxisFile = args[2];
	
		Config config = ConfigUtils.loadConfig(configfile, new TaxiConfigGroup());
		config.controler().setRunId(RUNID);
		String outPutDir = config.controler().getOutputDirectory()+"/"+RUNID+"/"; 
		config.controler().setOutputDirectory(outPutDir);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		TaxiConfigGroup tcg = (TaxiConfigGroup) config.getModule("taxi");
		tcg.setTaxisFile(taxisFile);
		
		VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
		
		
		walk.setDistance(1000);
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
		config.transitRouter().setSearchRadius(3000);
		config.transitRouter().setExtensionRadius(0);
		
	   TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
       config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
       config.checkConsistency();

       Scenario scenario = ScenarioUtils.loadScenario(config);
       TaxiData taxiData = new TaxiData();
       new VehicleReader(scenario.getNetwork(), taxiData).readFile(taxiCfg.getTaxisFileUrl(config.getContext()).getFile());
       Controler controler = new Controler(scenario);
       controler.addOverridingModule(new TaxiModule(taxiData));
       double expAveragingAlpha = 0.05;//from the AV flow paper 

       controler.addOverridingModule(
               VrpTravelTimeModules.createTravelTimeEstimatorModule(expAveragingAlpha));
       controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));
       controler.addOverridingModule(new VariableAccessTransitRouterModule());
//       controler.addOverridingModule(new TripHistogramModule());
//       controler.addOverridingModule(new OTFVisLiveModule());

       controler.run();


}
}
