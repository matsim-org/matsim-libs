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
package org.matsim.contrib.av.intermodal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
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
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunTaxiPTIntermodalExample {
	public static void main(String[] args) {
		new RunTaxiPTIntermodalExample().run(true);
	}

	public void run(boolean OTFVis) {
		Config config = ConfigUtils.loadConfig(
				"./src/main/resources/intermodal/config.xml",
				new TaxiConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		// yyyy in the long run, traffic dynamics should be "KWM" (or whatever it will be eventually called), and snapshotStyle should be set accordingly. kai, jan'17

		// yyyy Could you please javadoc the following?  EmissionsConfigGroup has an example how the explanatory strings
		// can be kept consistent between config file dump and javadoc.  Thx.  kai, jan'17
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		{
			VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
			taxi.setDistance(20000);
			taxi.setTeleported(false);
			taxi.setMode("taxi");
			vacfg.setAccessModeGroup(taxi);
		}
		{
			VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
			walk.setDistance(1000);
			walk.setTeleported(true);
			walk.setMode("walk");
			vacfg.setAccessModeGroup(walk);
		}
		config.addModule(vacfg);

		config.transitRouter().setSearchRadius(15000);
		config.transitRouter().setExtensionRadius(0);

		OTFVisConfigGroup otfvis = new OTFVisConfigGroup();
		otfvis.setDrawNonMovingItems(true);
		config.addModule(otfvis);

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		// ---
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TaxiData taxiData = new TaxiData();
		String taxiFileName = TaxiConfigGroup.get(config).getTaxisFileUrl(config.getContext()).getFile();
		new VehicleReader(scenario.getNetwork(), taxiData).readFile(taxiFileName);
		// ---
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new TaxiModule(taxiData));

//				// to replace by own dispatch module:
//				controler.addOverridingModule( new AbstractModule(){
//					@Override public void install() {
////						bind(TaxiOptimizerFactory.class).to(MyTaxiOptimizerFactory.class);
//						bind(TaxiOptimizerFactory.class).to(MyOtherTaxiOptimizerFactory.class) ;
//					}
//				} ) ;

		// yyyy can't we put the following into TaxiModule?  One can always override them anyways. kai, jan'17
		double expAveragingAlpha = 0.05;
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(expAveragingAlpha));
		controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		if (OTFVis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		// ---
		controler.run();
	}
}
