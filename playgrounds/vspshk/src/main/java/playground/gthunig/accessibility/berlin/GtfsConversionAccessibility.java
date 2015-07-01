package playground.gthunig.accessibility.berlin;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import playground.mzilske.gtfs.GtfsConverter;

public class GtfsConversionAccessibility {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		GtfsConverter gtfs = new GtfsConverter("C:/Users/Gabriel/workspace/otp-matsim/input/routeTest/gtfs/129384", scenario, new IdentityTransformation());
		
		gtfs.setDate(20150604);
		gtfs.convert();
	}

}
