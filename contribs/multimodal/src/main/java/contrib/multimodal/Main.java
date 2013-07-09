package contrib.multimodal;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import contrib.multimodal.tools.PrepareMultiModalScenario;

public class Main {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PrepareMultiModalScenario.run(scenario);
		Controler controler = new Controler(scenario);
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig());
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();	
	
		MultimodalSimulationTripRouterFactory tripRouterFactory = new MultimodalSimulationTripRouterFactory(
				controler, multiModalTravelTimes);
		MultimodalQSimFactory qSimFactory = new MultimodalQSimFactory(multiModalTravelTimes);
		controler.setTripRouterFactory(tripRouterFactory);
		controler.setMobsimFactory(qSimFactory);

	}

}
