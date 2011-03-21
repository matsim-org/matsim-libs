package playground.wrashid.lib.tools.network;

import java.io.IOException;

import playground.wrashid.lib.GeneralLib;

public class ConvertMATSimNetworkToKMZ {
	public static void main(String[] args) throws IOException {
		String networkFile="C:/tmp/output_network.xml.gz";
		//String networkFile="C:/data/workspace/matsim/test/input/org/matsim/core/utils/io/OsmNetworkReaderTest/adliswil.xml.gz";
		String outputKmzFileName="C:/eTmp/output_network.kmz";
		GeneralLib.convertMATSimNetworkToKmz(networkFile, outputKmzFileName);
	}
}
