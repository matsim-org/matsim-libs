package playground.dziemke.teach.tt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class KonvertiereNetwork_OsmToXml {

	/**
	 * main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String osm = "input/brandenburg-potsdam-merged.osm"; //"path-to-merged-network.osm";
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84, //Koordinatensystem mit L�ngen- und Breitengeraden, wie es bei OpenStreetMap genutzt wird
						"EPSG:3395"); //Koordinatensystem f�r Region Brandenburg in Metern, um Abst�nde messen zu k�nnen.
//						TransformationFactory.CH1903_LV03); //Koordinatensystem f�r Region Schweiz in Metern, um Abst�nde messen zu k�nnen.
		OsmNetworkReader onr = new OsmNetworkReader(net, ct);
		onr.parse(osm);
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write("input/brandenburg-potsdam-merged.xml"); //("output-path-for-network.xml");
	}

}