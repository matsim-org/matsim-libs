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
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;


public class PotsdamNet {
	
	public static void main(String[] args) {
		String osm = "./inputs/brandenburg.osm";
		
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		Network net = sc.getNetwork();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S); 
		
			
		
		TeachOsmNetworkReader onr = new TeachOsmNetworkReader(net,ct); //constructs a new openstreetmap reader
		onr.setHierarchyLayer(52.774, 12.398, 52.051, 13.774, 3);
		onr.setHierarchyLayer(52.5152, 12.8838, 52.3402, 13.1709, 6);
	
		try {
			onr.parse(osm); //starts the conversion from osm to matsim
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//at this point we already have a matsim network...
		new NetworkCleaner().run(net); //but may be there are isolated not connected links. The network cleaner removes those links
		
		new NetworkWriter(net).write("./inputs/network.xml");//here we write the network to a xml file

	}

}
