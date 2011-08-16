package playground.sergioo.NetworkVisualizer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.NetworkVisualizer.gui.SimpleNetworkWindow;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.SimpleNetworkPainter;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.publicTransport.PublicTransportNetworkPainter;

public class VisualizerRunner {

	/**
	 * @param args: 0 - Title, 1 - MATSim XML network file, 2 - MATSim XML public transport schedule file
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[1]);
		SimpleNetworkWindow window = null;
		if(args.length<3)
			window = new SimpleNetworkWindow(args[0], new SimpleNetworkPainter(scenario.getNetwork()));
		else {
			((ScenarioImpl)scenario).getConfig().scenario().setUseTransit(true);
			new TransitScheduleReader(scenario).readFile(args[2]);
			window = new SimpleNetworkWindow(args[0], new PublicTransportNetworkPainter(scenario.getNetwork(),((ScenarioImpl)scenario).getTransitSchedule()));
		}
		window.setVisible(true);
	}

}
