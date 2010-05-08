package org.matsim.core.api.experimental;

import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**<p>
 * Factory for the ScenarioLoader (what a surprise).
 * </p><p>
 * Note: There is deliberately NO constructor that takes a Scenario argument to avoid confusion what to do
 * with Scenario containers that already contain information.
 */
public class ScenarioLoaderFactoryImpl implements MatsimFactory {
	// yyyy if we use "Impl" for classes that should not be instantiated directly, then this should not be an "Impl".  kai, may'10

	public ScenarioLoader createScenarioLoader(String configFileName) {
		return new ScenarioLoaderImpl( configFileName ) ;
	}

//	public ScenarioLoaderI createScenarioLoader(String configFileName, Scenario scenario) {
//		return new ScenarioLoader(configFileName, scenario);
//	}

}
