package playground.polettif.publicTransitMapping.workbench.santiago;


import playground.polettif.publicTransitMapping.osm.MultimodalNetworkCreatorPT;

public class RunOsm2Network {

	public static void main(String[] args) {
		String base = "C:/Users/Flavio/Desktop/data/santiago/";
		MultimodalNetworkCreatorPT.run(base + "osm/santiago_chile.osm", base + "network/santiago_chile.xml.gz", "EPSG:32719");
	}
}
