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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class EvacPatnaControler {

	public static void main(String[] args) {

		String configFile ;
		LinkDynamics linkDynamics;
		String outDir;
		boolean isSeepModeStorageFree ;

		if(args.length==0){
			configFile = "../../../../repos/runs-svn/patnaIndia/run109/input/patna_evac_config.xml.gz";
			outDir = "../../../../repos/runs-svn/patnaIndia/run109/100pct/";
			linkDynamics = LinkDynamics.PassingQ;
			isSeepModeStorageFree = false;
		} else {
			configFile = args[0];
			outDir = args[1];
			linkDynamics = LinkDynamics.valueOf(args[2]);
			isSeepModeStorageFree = Boolean.valueOf(args[3]);
		}

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outDir);

		config.qsim().setLinkDynamics(linkDynamics.name());
		config.qsim().setSeepModeStorageFree(isSeepModeStorageFree);
		config.controler().setOutputDirectory(config.controler().getOutputDirectory()+"/evac_"+linkDynamics.name()+"/");
		config.controler().setDumpDataAtEnd(true);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.vspExperimental().setWritingOutputEvents(true);

		Scenario sc = ScenarioUtils.loadScenario(config); 

		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		PatnaUtils.createAndAddVehiclesToScenario(sc, PatnaUtils.URBAN_MAIN_MODES);

		final Controler controler = new Controler(sc);

		final RandomizingTimeDistanceTravelDisutilityFactory builder_bike =  new RandomizingTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);

				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());					
			}
		});

		controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListner.class);
			}
		});

		controler.run();
	}
}