/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class EvacPatnaControler {

	public static void main(String[] args) {

		String configFile ;
		boolean  isUsingSeepage;
		String outDir;
		boolean congestionPricing;

		if(args.length==0){
			configFile = "../../../repos/runs-svn/patnaIndia/run105/input/patna_evac_config.xml.gz";
			isUsingSeepage = false;
			outDir = "../../../repos/runs-svn/patnaIndia/run105/100pct/";
			congestionPricing = true;
		} else {
			configFile = args[0];
			isUsingSeepage = Boolean.valueOf(args[1]);
			outDir = args[2];
			congestionPricing = Boolean.valueOf(args[3]);
		}

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outDir);

		if(congestionPricing) config.controler().setOutputDirectory(config.controler().getOutputDirectory()+"/congestionPricing/");
		
		if(isUsingSeepage){	
			config.qsim().setLinkDynamics(LinkDynamics.SeepageQ.toString());
			config.qsim().setSeepMode("bike");
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);
			
			String outputDir = config.controler().getOutputDirectory()+"/evac_seepage/";
			config.controler().setOutputDirectory(outputDir);
		} else {
			String outputDir = config.controler().getOutputDirectory()+"/evac_passing/";
			config.controler().setOutputDirectory(outputDir);
		}

		Scenario sc = ScenarioUtils.loadScenario(config); 

		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		
		Logger.getLogger(EvacPatnaControler.class).error("Check the modes in the following call first. jan 16");
//		PatnaUtils.createAndAddVehiclesToScenario(sc);

		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		if(isUsingSeepage){
			Logger.getLogger(EvacPatnaControler.class).error("Removed seepage network factory. Should work without it if not, check is passing is allowed.");
		}
		
		if(congestionPricing) {
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
//			services.addControlerListener(new MarginalCongestionPricingContolerListener(services.getScenario(),tollHandler, new CongestionHandlerImplV6(services.getEvents(), (ScenarioImpl)services.getScenario()) ));
		}
		controler.run();
	}
}
