package playground.wrashid.lib.main;

import java.io.IOException;

import playground.wrashid.lib.GeneralLib;

public class KMLNetworkWriterMain {
	public static void main(String[] args) throws IOException {
		GeneralLib.writeNetworkToKmz("C:/data/workspace/playgrounds/mzilske/inputs/schweiz/zurich-switzerland.xml.gz", "C:/data/workspace/playgrounds/mzilske/inputs/schweiz/zurich.kmz");
	}
}
