package playground.mzilske.sotm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunCemdapBerlin {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("/Users/michaelzilske/wurst/berlin-1pct/config_be_1pct.xml");
		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		config.qsim().setTimeStepSize(15);
		otfVisConfigGroup.setMapOverlayMode(true);
		otfVisConfigGroup.setDelay_ms(0);
//		otfVisConfigGroup.setRenderImages(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		OTFVis.playScenario(scenario);

	}

}
