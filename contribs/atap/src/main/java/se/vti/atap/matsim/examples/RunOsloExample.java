/**
 * se.vti.atap.matsim.examples
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
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.atap.matsim.ATAP;
import se.vti.atap.matsim.ATAPConfigGroup;
import se.vti.emulation.EmulationConfigGroup;

/**
 * @author GunnarF
 */
public class RunOsloExample {

	public static void main(String[] args) {
		ATAP atap = new ATAP();

		Config config = ConfigUtils.loadConfig("./oslo/input/oslo_config_atap_example.xml", new ATAPConfigGroup(),
				new EmulationConfigGroup());
		atap.configure(config);

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		atap.configure(controler);

//		controler.addControlerListener(new StartupListener() {
//			@Override
//			public void notifyStartup(StartupEvent event) {
//				// TODO 2025-05-21 Changed this when updating to matsim 2024. Gunnar
////				Logger.getLogger(EventsManagerImpl.class).setLevel(Level.OFF);
//				Configurator.setLevel(EventsManagerImpl.class, Level.OFF);
//			}
//		});

		controler.run();
	}

}
