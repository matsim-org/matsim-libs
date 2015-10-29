package playground.sergioo.visualizer2D2012;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.SimpleSelectionNetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.publicTransport.PublicTransportNetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.publicTransportCapacity.PublicTransportNetworkWindow;

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
			window = new SimpleNetworkWindow(args[0], new SimpleSelectionNetworkPainter(scenario.getNetwork()));
		/*else if (args.length<4){
			SimpleSelectionNetworkPainter networkPainter = new SimpleSelectionNetworkPainter(scenario.getNetwork(), Color.LIGHT_GRAY, new BasicStroke(0.5f), new Color(0.5f,0,0), Color.BLACK, new BasicStroke(2));
			RoadPricingScheme roadPricingScheme = new RoadPricingScheme();
			new RoadPricingReaderXMLv1(roadPricingScheme).parse(args[2]);
			Collection<Link> selectedLinks = new ArrayList<Link>();
			for(Id<Link> id:roadPricingScheme.getLinkIdSet())
				selectedLinks.add(scenario.getNetwork().getLinks().get(id));
			networkPainter.selectLinks(selectedLinks);
			window = new SimpleNetworkWindow(args[0], networkPainter); 
		}*/
		else { 
			((MutableScenario)scenario).getConfig().transit().setUseTransit(true);
			new TransitScheduleReader(scenario).readFile(args[2]);
			if(args.length<4)
				window = new PublicTransportNetworkWindow(args[0], new PublicTransportNetworkPainter(scenario.getNetwork(),((MutableScenario)scenario).getTransitSchedule()));
			else {
				CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
				Coord upLeft = coordinateTransformation.transform(new Coord(Double.parseDouble(args[4]), Double.parseDouble(args[5])));
				Coord downRight = coordinateTransformation.transform(new Coord(Double.parseDouble(args[6]), Double.parseDouble(args[7])));
				window = new PublicTransportNetworkWindow(args[0], new PublicTransportNetworkPainter(scenario.getNetwork(),((MutableScenario)scenario).getTransitSchedule()), new File(args[3]), new double[]{upLeft.getX(), upLeft.getY()}, new double[]{downRight.getX(), downRight.getY()});
			}
		}
		window.setVisible(true);
	}

}
