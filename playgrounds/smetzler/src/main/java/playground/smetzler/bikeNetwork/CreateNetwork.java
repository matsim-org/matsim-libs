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
		
		String DHDN_GK4 = "EPSG:31468"; //String DHDN = "EPSG:3068";
		String WGS84 = "EPSG:4326";
		
		String inputOSM = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/tempelhof/tempelhof.osm";
	//	String outputXML = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/skalitzer/skalitzer_MATsim.xml";
	//  String outputBikeXML = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/skalitzer/skalitzer_bikeatt.xml";
		
		String outputXML =     "../smetzler/input/network/tempelhof_MATsim.xml";
		String outputBikeXML = "../smetzler/input/network/tempelhof_bikeatt_Oneway.xml";
		
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = 
				TransformationFactory.getCoordinateTransformation(WGS84, DHDN_GK4); //TransformationFactory.WGS84

		//wie kann ich die bike-Interfaces einbringen??
		BikeCustomizedOsmNetworkReader bikeNetworkReader = new BikeCustomizedOsmNetworkReader(net,ct);
		bikeNetworkReader.parse(inputOSM); 
		
		//new NetworkCleaner().run(net);
		new NetworkWriter(net).write(outputXML);
		
		//new bike attributes writer
		new ObjectAttributesXmlWriter(bikeNetworkReader.getBikeAttributes()).writeFile(outputBikeXML);
	}


}

