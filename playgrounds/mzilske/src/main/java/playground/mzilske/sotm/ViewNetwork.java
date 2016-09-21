package playground.mzilske.sotm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class ViewNetwork {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfVisConfigGroup.setMapOverlayMode(true);
		config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("/Users/michaelzilske/wurst/hermann-wurst.xml");
		OTFVis.playScenario(scenario);
	}

}
