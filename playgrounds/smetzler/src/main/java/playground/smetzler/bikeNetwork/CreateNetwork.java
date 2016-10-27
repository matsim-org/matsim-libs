package playground.smetzler.bikeNetwork;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateNetwork {

	public static void main(String[] args) {
		
		String WGS84 = "EPSG:4326";
		String DHDN_GK4 = "EPSG:31468";
		
		
		
//		String szenarioname= "BerlinBikeNet_mod";
//		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/berlin/" + szenarioname + ".osm";
//	//	String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/unequil/network/"+ szenarioname + ".osm";
//		
////		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/network/equil_eigenbau_MATsim.xml";
////		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/equil_eigenbau_bikeObjectAtt.xml";
//		
//		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/network/" + szenarioname + "_MATsim.xml.gz";
//		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/network/" + szenarioname + "_bikeObjectAtt.xml.gz";
		
		
		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/berlin/massnahmen/BerlinBikeNet_mod_falke.osm";
		String szenarioname= "Berlin_falke";
		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/network/massnahmen/" + szenarioname + "_MATsim.xml.gz";
		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/network/massnahmen/" + szenarioname + "_bikeObjectAtt.xml.gz";

		
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		
		Network bikeNetwork = NetworkUtils.createNetwork();
		Network carNetwork = NetworkUtils.createNetwork();
		
		CoordinateTransformation ct = 
				TransformationFactory.getCoordinateTransformation(WGS84, DHDN_GK4); //TransformationFactory.WGS84

		BikeCustomizedOsmNetworkReader bikeNetworkReader = new BikeCustomizedOsmNetworkReader(bikeNetwork,ct);
		bikeNetworkReader.constructBikeNetwork(inputOSM); 
		
		//new bike attributes writer
		new ObjectAttributesXmlWriter(bikeNetworkReader.getBikeAttributes()).writeFile(outputBikeXML);

		
		BikeCustomizedOsmNetworkReader networkReader = new BikeCustomizedOsmNetworkReader(carNetwork,ct);
		networkReader.constructCarNetwork(inputOSM);
		
		
		new NetworkCleaner().run(bikeNetwork);
		new NetworkCleaner().run(carNetwork);
		
		Network mergedNetwork = NetworkUtils.createNetwork();
		
		List<Link> bikeLinks = new ArrayList<Link>(bikeNetwork.getLinks().values());
		List<Link> carLinks = new ArrayList<Link>(carNetwork.getLinks().values());
		for (Node node : new ArrayList<Node>(bikeNetwork.getNodes().values())) {
			bikeNetwork.removeNode(node.getId());
			mergedNetwork.addNode(node);
		}
		for (Node node : new ArrayList<Node>(carNetwork.getNodes().values())) {
			carNetwork.removeNode(node.getId());
			if (!mergedNetwork.getNodes().containsKey(node.getId())) {
				mergedNetwork.addNode(node);
			}
		}
		
		for (Link link : bikeLinks) {
			mergedNetwork.addLink(link);
		}
		for (Link link : carLinks) {
			mergedNetwork.addLink(link);
		}
		
		
		
		new NetworkWriter(mergedNetwork).write(outputXML);
//		new NetworkWriter(bikeNetwork).write(outputXML);
	}


}

