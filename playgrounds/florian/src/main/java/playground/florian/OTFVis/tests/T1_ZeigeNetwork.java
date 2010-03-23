package playground.florian.OTFVis.tests;

import org.matsim.run.OTFVis;

public class T1_ZeigeNetwork {

	private static String network = "./test/input/playground/florian/OTFVis/network.xml";
	
	public static void main(String[] args) {
		String[] net = new String[1];
		net[0] = network;
		OTFVis.playNetwork(net);
	}

}
