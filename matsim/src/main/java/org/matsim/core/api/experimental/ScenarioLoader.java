package org.matsim.core.api.experimental;

import org.matsim.api.core.v01.Scenario;

public interface ScenarioLoader {

	public Scenario loadScenario();
	
	public Scenario getScenario() ;

}