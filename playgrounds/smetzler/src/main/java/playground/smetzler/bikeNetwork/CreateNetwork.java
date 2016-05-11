package playground.smetzler.bikeNetwork;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateNetwork {

	public static void main(String[] args) {
		
		String WGS84 = "EPSG:4326";
		String DHDN_GK4 = "EPSG:31468"; //String DHDN = "EPSG:3068";
		
		
		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/sonstiges/network_sonstiges/innenring/innenring.osm";
//		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/sonstiges/network_sonstiges/berlin/berlin-latest.osm";
		

		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/network/innenring_MATsim_bikeAndCar_EUDEM.xml";
		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/innenring_bikeatt_bikeAndCar_EUDEM.xml";
//		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/network/berlin_MATsim.xml";
//		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/berlin_bikeatt.xml";
		
		
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = 
				TransformationFactory.getCoordinateTransformation(WGS84, DHDN_GK4); //TransformationFactory.WGS84

		//wie kann ich die bike-Interfaces einbringen??
		BikeCustomizedOsmNetworkReader bikeNetworkReader = new BikeCustomizedOsmNetworkReader(net,ct);
		bikeNetworkReader.parse(inputOSM); 
		
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write(outputXML);
		
		//new bike attributes writer
		new ObjectAttributesXmlWriter(bikeNetworkReader.getBikeAttributes()).writeFile(outputBikeXML);
	}


}

