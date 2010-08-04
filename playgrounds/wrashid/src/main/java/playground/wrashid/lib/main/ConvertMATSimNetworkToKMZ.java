package playground.wrashid.lib.main;

import java.io.IOException;

import playground.wrashid.lib.GeneralLib;

public class ConvertMATSimNetworkToKMZ {
	public static void main(String[] args) throws IOException {
		String networkFile="C:/data/workspace/playgrounds/mzilske/inputs/schweiz/zurich-switzerland.xml";
		//String networkFile="C:/data/workspace/matsim/test/input/org/matsim/core/utils/io/OsmNetworkReaderTest/adliswil.xml.gz";
		String outputKmzFileName="C:/data/workspace/matsim/test/input/org/matsim/core/utils/io/OsmNetworkReaderTest/adliswil.kmz";
		GeneralLib.convertMATSimNetworkToKmz(networkFile, outputKmzFileName);
	}
}
