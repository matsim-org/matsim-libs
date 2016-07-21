package org.matsim.vis.otfvis.checklists;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class T8_Map {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("test/scenarios/berlin/config.xml");
		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfVisConfigGroup.setMapOverlayMode(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		OTFVis.playScenario(scenario);
	}

}
