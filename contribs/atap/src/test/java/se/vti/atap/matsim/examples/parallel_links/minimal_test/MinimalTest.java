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
package se.vti.atap.matsim.examples.parallel_links.minimal_test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import se.vti.atap.matsim.examples.parallel_links.ScenarioCreator;
import se.vti.emulation.EmulationConfigGroup;

/**
 * @author GunnarF
 */
public class MinimalTest {

	@TempDir
    private Path tempDir;

	   @Test
	    void testFileChecksum() throws Exception {
		   
			String scenarioFolder = this.tempDir.toString();
			double sizeFactor = 3.0;
			double inflowDuration_s = 900.0;

			ScenarioCreator factory = new ScenarioCreator(inflowDuration_s, sizeFactor);
			factory.setBottleneck(0, 500.0);
			factory.setBottleneck(1, 500.0);
			factory.setOD(2000, 0, 1);

			Config config = factory.createConfig();
			config.controller().setOutputDirectory(Paths.get(scenarioFolder, "output").toString());
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(3);
			config.network().setInputFile("network.xml");
			config.plans().setInputFile("population.xml");
			config.travelTimeCalculator().setTraveltimeBinSize(60);
			config.qsim().setStuckTime(Double.POSITIVE_INFINITY);
			config.addModule(new EmulationConfigGroup());
			config.routing().setNetworkRouteConsistencyCheck(NetworkRouteConsistencyCheck.disable);

			ATAPConfigGroup atapConfig = new ATAPConfigGroup();
			atapConfig.setReplannerIdentifier(ReplannerIdentifierType.ATAP_EXACT_DISTANCE);
			atapConfig.setMaxMemory(4);
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
			
	        String expectedChecksum = "8e6a6c65e59738357f702d5478b3f954";
			try (InputStream is = Files.newInputStream(Paths.get(scenarioFolder, "output", "output_events.xml.gz"))) {
					String actualChecksum = DigestUtils.md5Hex(is);
					 assertEquals(expectedChecksum, actualChecksum, "File checksum does not match: " + actualChecksum);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	    }
}
