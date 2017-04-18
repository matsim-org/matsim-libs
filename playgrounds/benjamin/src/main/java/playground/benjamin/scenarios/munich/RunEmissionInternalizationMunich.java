/* *********************************************************************** *
 * project: org.matsim.*
 * RunMunich1pct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.vsp.airPollution.flatEmissions.InternalizeEmissionsControlerListener;

/**
 * @author benjamin
 *
 */
public class RunEmissionInternalizationMunich {

	static String configFile;
	static String emissionCostFactor;
	static String emissionEfficiencyFactor;
	static String considerCO2Costs;


	public static void main(String[] args) {
//		configFile = "../../detailedEval/internalization/munich1pct/input/config_munich_1pct.xml";
//		emissionCostFactor = "1.0";
//		emissionEfficiencyFactor = "1.0";
//		considerCO2Costs = "true";

		configFile = args[0];
		emissionCostFactor = args[1];
		emissionEfficiencyFactor = args[2];
		considerCO2Costs = args[3];

		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		ConfigReader confReader = new ConfigReader(config);
		confReader.readFile(configFile);

		EmissionsConfigGroup ecg = ( (EmissionsConfigGroup) config.getModules().get(EmissionsConfigGroup.GROUP_NAME));
		ecg.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		ecg.setEmissionCostMultiplicationFactor(Double.parseDouble(emissionCostFactor));
		ecg.setConsideringCO2Costs(Boolean.parseBoolean(considerCO2Costs));

		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();


		final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(
        );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EmissionModule.class).asEagerSingleton();
				bind(EmissionCostModule.class).asEagerSingleton();
				addControlerListenerBinding().to(InternalizeEmissionsControlerListener.class);

				bindCarTravelDisutilityFactory().toInstance(emissionTducf);
			}
		});

		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
	}
}