package playground.mzilske.teach;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;


public class Zurich {
	
	public static void main(String[] args) {
		String osm = "./inputs/schweiz-2/merged-network.osm";
		
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		Network net = sc.getNetwork();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S); 
		
			
		
		OsmNetworkReader onr = new OsmNetworkReader(net,ct); //constructs a new openstreetmap reader
		onr.setHierarchyLayer(48.15, 5.71, 45.41, 11, 6);
//		onr.setHierarchyLayer(47.701, 8.346, 47.146, 9.019, 6);
	
		try {
			onr.parse(osm); //starts the conversion from osm to matsim
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Converted.");
		
		//at this point we already have a matsim network...
		new NetworkCleaner().run(net); //but may be there are isolated not connected links. The network cleaner removes those links
		
		new NetworkWriter(net).write("./inputs/schweiz-2/network.xml");//here we write the network to a xml file

	}

}
