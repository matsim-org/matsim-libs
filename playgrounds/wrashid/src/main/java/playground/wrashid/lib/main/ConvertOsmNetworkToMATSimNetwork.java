package playground.wrashid.lib.main;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.network.Network;
import org.xml.sax.SAXException;

import playground.wrashid.lib.GeneralLib;

public class ConvertOsmNetworkToMATSimNetwork {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String inputOsmNetworkPathName="C:/data/workspace/matsim/test/input/org/matsim/core/utils/io/OsmNetworkReaderTest/adliswil.osm.gz";
		String outputMATSimNetworkFilePathName="C:/data/workspace/matsim/test/input/org/matsim/core/utils/io/OsmNetworkReaderTest/adliswil.xml.gz";
		
		
		Network network = GeneralLib.convertOsmNetworkToMATSimNetwork(inputOsmNetworkPathName);
		GeneralLib.writeNetwork(network, outputMATSimNetworkFilePathName);
	}

}
