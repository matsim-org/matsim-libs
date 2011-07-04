package playground.gregor.multidestpeds.denistyestimation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.events.XYZEventsFileReader;

public class Test {

	public static void main (String [] args) {

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		EventsManager events = EventsUtils.createEventsManager();
		NNGaussianKernelEstimator est = new DensityEstimatorFactory(events,sc).createDensityEstimator();




		events.addHandler(est);

		XYZEventsFileReader reader = new XYZEventsFileReader(events);
		reader.parse("/Users/laemmel/devel/dfg/output/ITERS/it.0/0.events.xml.gz");

	}

}
