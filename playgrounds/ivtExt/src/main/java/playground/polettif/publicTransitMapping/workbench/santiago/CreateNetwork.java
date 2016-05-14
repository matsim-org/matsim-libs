package playground.polettif.publicTransitMapping.workbench.santiago;


import org.matsim.api.core.v01.network.Network;
import playground.polettif.publicTransitMapping.osm.MultimodalNetworkCreatorPT;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.Collections;

public class CreateNetwork {

	public static void main(String[] args) {
		String base = "E:/data/santiago/";

		// create network from osm
		MultimodalNetworkCreatorPT.run(base + "osm/santiago_chile.osm", base + "network/santiago_chile_osm.xml.gz", "EPSG:32719");


		// filter provided network
		Network provNetwork = NetworkTools.filterNetworkByLinkMode(NetworkTools.loadNetwork(base + "scenario/input_original/network_merged_cl.xml.gz"), Collections.singleton("car"));
		NetworkTools.writeNetwork(provNetwork, base+"network/santiago_chile_simplified.xml.gz");
	}
}
