package playground.smetzler.bikeNetworkUnequil;

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

public class CreateNetwork_Unequil {

	public static void main(String[] args) {
		
		String WGS84 = "EPSG:4326";
		String DHDN_GK4 = "EPSG:31468"; //String DHDN = "EPSG:3068";
		
		
		//String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/berlin/BerlinBikeNet_mod.osm";
		
		//keepPaths line 489 !!!!!!!!!!!!!!!!!!
		//only create bike links
		//ohne slope bzw. slope aus attributen
		String szenarioname= "unequil_9slopeCyclewayTertiary";
		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/unequil/network/"+ szenarioname + ".osm";
		
//		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/network/equil_eigenbau_MATsim.xml";
//		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/equil_eigenbau_bikeObjectAtt.xml";
		
		String outputXML =     "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/unequil/network/" + szenarioname + "_MATsim.xml";
		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/unequil/network/" + szenarioname + "_bikeObjectAtt.xml";
		
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		
		Network bikeNetwork = NetworkUtils.createNetwork();
		Network carNetwork = NetworkUtils.createNetwork();
		
		CoordinateTransformation ct = 
				TransformationFactory.getCoordinateTransformation(WGS84, DHDN_GK4); //TransformationFactory.WGS84

		//wie kann ich die bike-Interfaces einbringen??
		BikeCustomizedOsmNetworkReader_Unequil bikeNetworkReader = new BikeCustomizedOsmNetworkReader_Unequil(bikeNetwork,ct);
		bikeNetworkReader.constructBikeNetwork(inputOSM); 
		
		//new bike attributes writer
		new ObjectAttributesXmlWriter(bikeNetworkReader.getBikeAttributes()).writeFile(outputBikeXML);

		
		BikeCustomizedOsmNetworkReader_Unequil networkReader = new BikeCustomizedOsmNetworkReader_Unequil(carNetwork,ct);
		networkReader.constructCarNetwork(inputOSM);
		
		
		new NetworkCleaner().run(bikeNetwork);
		new NetworkCleaner().run(carNetwork);
		
//		Network mergedNetwork = NetworkUtils.createNetwork();
//		
//		List<Link> bikeLinks = new ArrayList<Link>(bikeNetwork.getLinks().values());
//		List<Link> carLinks = new ArrayList<Link>(carNetwork.getLinks().values());
//		for (Node node : new ArrayList<Node>(bikeNetwork.getNodes().values())) {
//			bikeNetwork.removeNode(node.getId());
//			mergedNetwork.addNode(node);
//		}
//		for (Node node : new ArrayList<Node>(carNetwork.getNodes().values())) {
//			carNetwork.removeNode(node.getId());
//			if (!mergedNetwork.getNodes().containsKey(node.getId())) {
//				mergedNetwork.addNode(node);
//			}
//		}
//		
//		for (Link link : bikeLinks) {
//			mergedNetwork.addLink(link);
//		}
//		for (Link link : carLinks) {
//			mergedNetwork.addLink(link);
//		}
		
		
		
	//	new NetworkWriter(mergedNetwork).write(outputXML);
		new NetworkWriter(bikeNetwork).write(outputXML);
	}


}

