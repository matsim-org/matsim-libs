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
package playground.agarwalamit.mixedTraffic.patnaIndia;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vehicles.VehicleWriterV1;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.PatnaConfigGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.PatnaVehiclesGenerator;
import playground.agarwalamit.utils.plans.BackwardCompatibilityForRouteType;

/**
 * @author amit
 */

public class PatnaControler {

	private static final String inputFilesDir = "../../../../repos/runs-svn/patnaIndia/run108/input/";
	private static final String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/output/";

	public static void main(String[] args) {
		PatnaConfigGenerator configGenerator = new PatnaConfigGenerator();
		configGenerator.createBasicConfigSettings();
		Config config = configGenerator.getPatnaConfig();

		config.network().setInputFile(inputFilesDir+"/network_diff_linkSpeed.xml.gz");

		BackwardCompatibilityForRouteType bcrt = new BackwardCompatibilityForRouteType(inputFilesDir+"/SelectedPlansOnly.xml", PatnaConstants.mainModes);
		bcrt.startProcessing();
		String plansFile = inputFilesDir+"/selectedPlans_diff_tripPurpose.xml.gz";
		bcrt.writePopOut(plansFile);

		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		PatnaVehiclesGenerator pvg = new PatnaVehiclesGenerator(plansFile);
		pvg.createVehicles();
		String patnaVehicles = inputFilesDir+"/patnaVehicles.xml.gz";
		new VehicleWriterV1(pvg.getPatnaVehicles()).writeFile(patnaVehicles);

		config.plans().setInputFile(plansFile);
		config.counts().setCountsFileName(inputFilesDir+"/countsCarMotorbikeBike.xml");
		config.vehicles().setVehiclesFile(patnaVehicles);

		config.controler().setOutputDirectory(outputDir);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Controler controler = new Controler(config);
		controler.setDumpDataAtEnd(true);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
			}
		});
		controler.run();
	}
}
