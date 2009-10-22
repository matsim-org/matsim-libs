package org.matsim.core.api.experimental;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioLoader;

public class ScenarioLoaderFactoryImpl {

	public ScenarioLoaderI createScenarioLoader(String configFileName) {
		return new ScenarioLoader( configFileName ) ;
	}

	public ScenarioLoaderI createScenarioLoader(String configFileName, Scenario scenario) {
		return new ScenarioLoader(configFileName, scenario);
	}

}
