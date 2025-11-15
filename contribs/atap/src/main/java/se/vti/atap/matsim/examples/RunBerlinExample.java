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
package se.vti.atap.matsim.examples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.atap.matsim.ATAP;
import se.vti.atap.matsim.ATAPConfigGroup;
import se.vti.emulation.EmulationConfigGroup;

/**
 * Berlin scenario used in the article: <i>"A simulation heuristic for traveler-
 * and vehicle-discrete dynamic traffic assignment"</i>.
 *
 * <b>Scenario data:</b>
 * https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/zz_archive/2014-08-01_car_1pct
 * 
 * @author Gunnar Flötteröd
 */

public class RunBerlinExample {

	public static void main(String[] args) {

		// Only the config file is in this repo, network and population URL indicated in
		// header and config file.
		String pathToConfigInResources = "./src/test/resources/se/vti/atap/matsim/examples/";
		String configFileName = pathToConfigInResources + "berlin_config_atap_example.xml";

		// This class is the entry point to all ATAP functionality.
		ATAP atap = new ATAP();

		// MATSim standard: load configuration file.
		Config config = ConfigUtils.loadConfig(configFileName, new ATAPConfigGroup(), new EmulationConfigGroup());

		// ATAP scans the config, configures internally, and reorganizes and -weights
		// the strategies for greatest speed.
		atap.configure(config);

		// MATSim standard: load scenario file, create controler.
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// ATAP installs itself in the controller.
		atap.configure(controler);

		// MATSim standard: run the simulation.
		controler.run();
	}

}
