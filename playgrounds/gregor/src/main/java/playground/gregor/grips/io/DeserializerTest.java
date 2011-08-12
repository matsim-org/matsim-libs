package playground.gregor.grips.io;

import java.net.MalformedURLException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.grips.helper.Building;

public class DeserializerTest {
	public static void main(String [] args) throws JAXBException, MalformedURLException, SAXException {

		//		String input = "/Users/laemmel/tmp/network.xml";
		//		String output = "/Users/laemmel/tmp/matsimNetwork.xml";
		//		Network netw = new NetworkDeserializer().deserialize(input);
		//		new NetworkWriter(netw).write(output);

		String bl = "http://localhost:8080/geoserver/hamburg/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=hamburg:buildings&maxFeatures=50&outputFormat=text/xml;%20subtype=gml/3.1.1";
		//		String bl = "http://129.206.66.244:8081/geoserver/esdl/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=esdl:building&maxFeatures=50&outputFormat=text/xml;%20subtype=gml/3.1.1";
		//		String buildings = "/Users/laemmel/tmp/ows.xml";
		List<Building> blds = new BuildingsDeserializer().deserialize(bl);
		System.out.println(blds.size());

		String evacArea = "http://129.206.66.244:8081/geoserver/esdl/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=esdl:evacuationarea&maxFeatures=50";
		Polygon p = new EvacuationAreaDeserializer().deserialize(evacArea);
	}
}
