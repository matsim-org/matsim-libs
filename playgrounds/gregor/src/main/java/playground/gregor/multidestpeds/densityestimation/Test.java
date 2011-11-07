package playground.gregor.multidestpeds.densityestimation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

public class Test {

	public static void main (String [] args) {

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		new ScenarioLoader2DImpl(sc).load2DScenario();
		EventsManager events = EventsUtils.createEventsManager();
		NNGaussianKernelEstimator est = new DensityEstimatorFactory(events,sc).createDensityEstimator();




		events.addHandler(est);

		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(events);
		reader.parse("/Users/laemmel/devel/dfg/output/ITERS/it.0/0.events.xml.gz");

	}

}
