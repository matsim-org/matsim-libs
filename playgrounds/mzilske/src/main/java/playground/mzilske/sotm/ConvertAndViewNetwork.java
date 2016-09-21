package playground.mzilske.sotm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class ConvertAndViewNetwork {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfVisConfigGroup.setMapOverlayMode(true);
		config.global().setCoordinateSystem("EPSG:31468");
		Scenario scenario = ScenarioUtils.createScenario(config);
		new OsmNetworkReader(scenario.getNetwork(), TransformationFactory.getCoordinateTransformation("WGS84", "EPSG:31468"), true).parse("/Users/michaelzilske/wurst/hermann.osm.xml");
		OTFVis.playScenario(scenario);
	}

}
