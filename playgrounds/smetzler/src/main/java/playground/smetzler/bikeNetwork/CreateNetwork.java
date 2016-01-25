package playground.smetzler.bikeNetwork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateNetwork {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	
		
		String DHDN = "EPSG:3068";
		String inputOSM = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/skalitzer/skalitzer_OSM.xml";
	//	String outputXML = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/skalitzer/skalitzer_MATsim.xml";
	//  String outputBikeXML = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/skalitzer/skalitzer_bikeatt.xml";
		
		String outputXML =     "../smetzler/input/network/skalitzer_MATsim.xml";
		String outputBikeXML = "../smetzler/input/network/skalitzer_bikeatt.xml";
		
		

		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = 
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, DHDN);


		//wie kann ich die bike-Interfaces einbringen??
		BikeCustomizedOsmNetworkReader bikeNetworkReader = new BikeCustomizedOsmNetworkReader(net, ct);
		bikeNetworkReader.parse(inputOSM); 
		

		//new NetworkCleaner().run(net);

		new NetworkWriter(net).write(outputXML);
		//new bike attributes writer
		new ObjectAttributesXmlWriter(bikeNetworkReader.getBikeAttributes()).writeFile(outputBikeXML);


	}


}

