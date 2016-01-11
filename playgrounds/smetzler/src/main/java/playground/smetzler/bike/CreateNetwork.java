package playground.smetzler.bike;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class CreateNetwork {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	
		
		String DHDN = "EPSG:3068";
		String inputOSM = "C:/Users/Ettan/13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/schlesi/map_schlesi.osm";
		String outputXML = "C:/Users/Ettan/13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/schlesi/out_cycletest.xml";

		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = 
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, DHDN);


		OsmNetworkReader onr = new OsmNetworkReader(net, ct);
		onr.parse(inputOSM); 
		
		System.out.println(net.getLinks());
		

		
//		System.out.println(onr.);
//		System.out.print("hello");
		//new NetworkCleaner().run(net);

		new NetworkWriter(net).write(outputXML);
		
	}

}

