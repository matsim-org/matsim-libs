/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example script that shows how to use SwissRailRaptor, a very fast
 * public transport routing algorithm.
 *
 * @author mrieser / SBB (Swiss Federal Railways)
 */
public class RunSwissRailRaptorExample {

	public static void main(String[] args) {
		String configFilename = args[0];
		Config config = ConfigUtils.loadConfig(configFilename);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// This is the important line:
		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.run();
	}

}
