package org.matsim.core.api.experimental;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioLoader;

public class ScenarioLoaderFactoryImpl {

	public ScenarioLoaderI createScenarioLoader(String configFileName) {
		return new ScenarioLoader( configFileName ) ;
	}

	public ScenarioLoaderI createScenarioLoader(String configFileName, Scenario scenario) {
		// TODO Auto-generated method stub
		//return null;
		throw new UnsupportedOperationException() ;
		// needs to be implemented in ScenarioLoader.
	}

}
