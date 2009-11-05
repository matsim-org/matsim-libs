package org.matsim.core.api.experimental;

import org.matsim.core.scenario.ScenarioLoader;

/**<p>
 * Factory for the ScenarioLoader (what a surprise).
 * </p><p>
 * Note: There is deliberately NO constructor that takes a Scenario argument to avoid confusion what to do
 * with Scenario containers that already contain information.
 */
public class ScenarioLoaderFactoryImpl {

	public ScenarioLoaderI createScenarioLoader(String configFileName) {
		return new ScenarioLoader( configFileName ) ;
	}

//	public ScenarioLoaderI createScenarioLoader(String configFileName, Scenario scenario) {
//		return new ScenarioLoader(configFileName, scenario);
//	}

}
