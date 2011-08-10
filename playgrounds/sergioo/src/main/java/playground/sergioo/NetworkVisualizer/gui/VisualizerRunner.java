package playground.sergioo.NetworkVisualizer.gui;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class VisualizerRunner {

	/**
	 * @param args: 0 - MATSim XML network file, 1 - Title
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile(args[0]);
		Network network = scenario.getNetwork();
		Window window = new Window(args[1],network);
		window.setVisible(true);
	}

}
