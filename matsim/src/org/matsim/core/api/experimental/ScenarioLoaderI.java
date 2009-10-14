package org.matsim.core.api.experimental;

import org.matsim.api.core.v01.Scenario;

public interface ScenarioLoaderI {

	public Scenario loadScenario();
	
	public Scenario getScenario() ;

}