package playground.gregor.grips.io;

import java.net.MalformedURLException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.xml.sax.SAXException;

import playground.gregor.grips.helper.Building;

public class DeserializerTest {
	public static void main(String [] args) throws JAXBException, MalformedURLException, SAXException {

		//		String input = "/Users/laemmel/tmp/network.xml";
		//		String output = "/Users/laemmel/tmp/matsimNetwork.xml";
		//		Network netw = new NetworkDeserializer().deserialize(input);
		//		new NetworkWriter(netw).write(output);


		String buildings = "/Users/laemmel/tmp/ows.xml";
		List<Building> blds = new BuildingsDeserializer().deserialize(buildings);
	}
}
