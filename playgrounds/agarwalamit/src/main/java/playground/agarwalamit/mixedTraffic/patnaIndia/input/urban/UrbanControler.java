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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.plans.BackwardCompatibilityForOldPlansType;

/**
 * @author amit
 */

public class UrbanControler {

	private static final String INPUT_FILE_DIR = "../../../../repos/runs-svn/patnaIndia/run108/input/";
	private static final String OUTPUT_DIR = "../../../../repos/runs-svn/patnaIndia/run108/output/t3/";

	public static void main(String[] args) {
		UrbanConfigGenerator configGenerator = new UrbanConfigGenerator();
		configGenerator.createBasicConfigSettings();
		Config config = configGenerator.getPatnaConfig();

		config.network().setInputFile(INPUT_FILE_DIR+"/network_diff_linkSpeed.xml.gz");

		BackwardCompatibilityForOldPlansType bcrt = new BackwardCompatibilityForOldPlansType(INPUT_FILE_DIR+"/SelectedPlansOnly.xml", PatnaUtils.URBAN_MAIN_MODES);
		bcrt.extractPlansExcludingLinkInfo();
		String plansFile = INPUT_FILE_DIR+"/selectedPlans_diff_tripPurpose.xml.gz";
		bcrt.writePopOut(plansFile);

		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		PatnaVehiclesGenerator pvg = new PatnaVehiclesGenerator(plansFile);
		Vehicles vehs = pvg.createAndReturnVehicles(PatnaUtils.URBAN_MAIN_MODES);
		String patnaVehicles = INPUT_FILE_DIR+"/patnaVehicles.xml.gz";
		new VehicleWriterV1(vehs).writeFile(patnaVehicles);

		config.plans().setInputFile(plansFile);
		config.counts().setInputFile(INPUT_FILE_DIR+"/countsCarMotorbikeBike.xml");
		config.vehicles().setVehiclesFile(patnaVehicles);

		config.controler().setOutputDirectory(OUTPUT_DIR);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutilityFactory builder =  new RandomizingTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
//				addTravelTimeBinding("bike").to(networkTravelTime());
//				addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder);
				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
			}
		});
		controler.run();
	}
}
