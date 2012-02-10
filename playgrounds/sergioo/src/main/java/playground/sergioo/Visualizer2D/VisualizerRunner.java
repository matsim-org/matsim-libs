package playground.sergioo.Visualizer2D;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.Visualizer2D.NetworkVisualizer.SimpleNetworkWindow;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.SimpleSelectionNetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.PublicTransport.PublicTransportNetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.PublicTransportCapacity.PublicTransportNetworkWindow;

public class VisualizerRunner {

	/**
	 * @param args: 0 - Title, 1 - MATSim XML network file, 2 - MATSim XML public transport schedule file
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[1]);
		LayersWindow window = null;
		if(args.length<3)
			window = new SimpleNetworkWindow(args[0], new SimpleSelectionNetworkPainter(scenario.getNetwork(), Color.BLACK));
		else { 
			((ScenarioImpl)scenario).getConfig().scenario().setUseTransit(true);
			new TransitScheduleReader(scenario).readFile(args[2]);
			if(args.length<4)
				window = new PublicTransportNetworkWindow(args[0], new PublicTransportNetworkPainter(scenario.getNetwork(),((ScenarioImpl)scenario).getTransitSchedule()));
			else {
				CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
				window = new PublicTransportNetworkWindow(args[0], new PublicTransportNetworkPainter(scenario.getNetwork(),((ScenarioImpl)scenario).getTransitSchedule()), new File(args[3]), coordinateTransformation.transform(new CoordImpl(Double.parseDouble(args[4]), Double.parseDouble(args[5]))), coordinateTransformation.transform(new CoordImpl(Double.parseDouble(args[6]), Double.parseDouble(args[7]))));
			}
		}
		window.setVisible(true);
	}

}
