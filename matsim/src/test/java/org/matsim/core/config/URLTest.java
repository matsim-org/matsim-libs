package org.matsim.core.config;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class URLTest {

	@Test
	public void testLoadWithURL() throws MalformedURLException {
		Config config = ConfigUtils.loadConfig(new URL("file:../examples/scenarios/equil/config.xml"));
		Scenario scenario = ScenarioUtils.loadScenario(config);
	}

}
