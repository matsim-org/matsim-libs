/**
 * se.vti.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.matsim.examples.parallel_links;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup.NetworkRouteConsistencyCheck;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import se.vti.atap.matsim.ATAP;
import se.vti.atap.matsim.ATAPConfigGroup;
import se.vti.atap.matsim.ATAPConfigGroup.ReplannerIdentifierType;
import se.vti.emulation.EmulationConfigGroup;

/**
 * 
 * @author GunnarF
 *
 */
public class ExampleRunner {

	public static void runSmallExampleScenario(String scenarioFolder, ReplannerIdentifierType replannerIdentifier) {
		double sizeFactor = 3.0;
		double inflowDuration_s = 900.0;

		ScenarioCreator factory = new ScenarioCreator(inflowDuration_s, sizeFactor);
		factory.setBottleneck(0, 500.0);
		factory.setBottleneck(1, 500.0);
		factory.setOD(2000, 0, 1);

		Config config = factory.createConfig();
		config.controller().setOutputDirectory(Paths.get(scenarioFolder, "output").toString());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(100);
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("population.xml");
		config.travelTimeCalculator().setTraveltimeBinSize(60);
		config.qsim().setStuckTime(Double.POSITIVE_INFINITY);
		config.addModule(new EmulationConfigGroup());
		config.routing().setNetworkRouteConsistencyCheck(NetworkRouteConsistencyCheck.disable);

		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(replannerIdentifier);
		atapConfig.setMaxMemory(4);
//		atapConfig.setKernelHalftime_s(60);
		atapConfig.setReduceLogging(true);
		config.addModule(atapConfig);

		Scenario scenario = factory.createScenario(config);

		if (scenarioFolder != null) {
			File folder = new File(scenarioFolder);
			if (folder.exists()) {
				try {
					FileUtils.cleanDirectory(folder);
				} catch (IOException e) {
					throw new RuntimeException();
				}
			} else {
				folder.mkdirs();
			}
			ConfigUtils.writeMinimalConfig(config, Paths.get(scenarioFolder, "config.xml").toString());
			NetworkUtils.writeNetwork(scenario.getNetwork(),
					Paths.get(scenarioFolder, scenario.getConfig().network().getInputFile()).toString());
			PopulationUtils.writePopulation(scenario.getPopulation(),
					Paths.get(scenarioFolder, scenario.getConfig().plans().getInputFile()).toString());
		}

		var atap = new ATAP();
		atap.configure(scenario.getConfig());
		var controler = new Controler(scenario);
		atap.configure(controler);
		controler.run();
	}

	public static void runSmallExampleWithUniform() {
		runSmallExampleScenario("./small-example/uniform", ReplannerIdentifierType.UNIFORM);
	}

	public static void runSmallExampleWithSorting() {
		runSmallExampleScenario("./small-example/sorting", ReplannerIdentifierType.SORTING);
	}

	public static void runSmallExampleWithProposed() {
		runSmallExampleScenario("./small-example/proposed", ReplannerIdentifierType.ATAP_EXACT_DISTANCE);
	}

	public static void main(String[] args) {
//		 System.out.println("UNIFORM METHOD");
//		 runSmallExampleWithUniform();

//		 System.out.println("SORTING METHOD");
//		 runSmallExampleWithSorting();

		 System.out.println("PROPOSED METHOD");
		 runSmallExampleWithProposed();
	}

}
