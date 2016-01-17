package org.matsim.core.scenario;


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public class ScenarioByConfigModule extends AbstractModule {
	@Override
	public void install() {
		Scenario scenario  = ScenarioUtils.createScenario(getConfig());
		ScenarioUtils.loadScenario(scenario) ;
		install(new ScenarioByInstanceModule(scenario));
	}
}
