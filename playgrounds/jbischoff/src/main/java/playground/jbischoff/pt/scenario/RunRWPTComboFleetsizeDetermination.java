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

import org.matsim.api.core.v01.*;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.*;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

import com.google.inject.AbstractModule;

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
		
		TaxiConfigGroup tcg = (TaxiConfigGroup) config.getModules().get(TaxiConfigGroup.GROUP_NAME);
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
	   taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
	   taxiCfg.setChangeStartLinkToLastLinkInSchedule(true);
       config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
       config.checkConsistency();

       Scenario scenario = ScenarioUtils.loadScenario(config);
       FleetImpl fleet = new FleetImpl();
       new VehicleReader(scenario.getNetwork(), fleet).readFile(taxiCfg.getTaxisFileUrl(config.getContext()).getFile());
       Controler controler = new Controler(scenario);

       VehicleType avVehType = new VehicleTypeImpl(Id.create("avVehicleType", VehicleType.class));
       avVehType.setFlowEfficiencyFactor(2);
       controler.addOverridingModule(new TaxiModule(avVehType));

       double expAveragingAlpha = 0.05;//from the AV flow paper 
       controler.addOverridingModule(
               VrpTravelTimeModules.createTravelTimeEstimatorModule(expAveragingAlpha));

       controler.addOverridingModule(new DvrpModule(TaxiModule.TAXI_MODE, fleet, new AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(DefaultTaxiOptimizerProvider.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
			}
		}, TaxiOptimizer.class));

       controler.addOverridingModule(new VariableAccessTransitRouterModule());
//       controler.addOverridingModule(new TripHistogramModule());
//       controler.addOverridingModule(new OTFVisLiveModule());

       controler.run();


}
}
