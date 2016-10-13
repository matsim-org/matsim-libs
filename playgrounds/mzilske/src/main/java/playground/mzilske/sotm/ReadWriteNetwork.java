package playground.mzilske.sotm;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class ReadWriteNetwork {

	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("/Users/michaelzilske/wurst/hermann-gk4.xml");
		new NetworkWriter(network).writeV1("/Users/michaelzilske/wurst/hermann-wurst.xml");
	}

}
