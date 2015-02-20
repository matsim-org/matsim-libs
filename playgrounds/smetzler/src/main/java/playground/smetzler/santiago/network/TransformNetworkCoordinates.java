package playground.smetzler.santiago.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

/**
 * 
 * @author aneumann, scnadine
 *
 */
public class TransformNetworkCoordinates {


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourceFile = "e:/_shared-svn/_data/santiago_pt_demand_matrix/network/santiago_tiny.xml";
		String targetFile = "e:/_shared-svn/_data/santiago_pt_demand_matrix/network/santiago_tiny_transformed.xml.gz";
		String sourceCoordSystem = "EPSG:3857"; // OSM WGS84
		String targetCoordSystem = "EPSG:24879"; // PSAD56 / UTM zone 19S
		
		// Manual correction. The coordinate system of the demand and transit stops is unknown. EPSG:24879 seems to be the best option but suffers from a constant offset
		double shiftX = -185.0;
		double shiftY = -375.0;
		
		
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		Network network = scenario.getNetwork();
		
		new MatsimNetworkReader(scenario).readFile(sourceFile);	
		
		GeotoolsTransformation ct = new GeotoolsTransformation(sourceCoordSystem, targetCoordSystem);
		
		for (Node node : network.getNodes().values()) {						
			Coord coord = node.getCoord();				
			Coord helperCoord = ct.transform(coord);
			node.getCoord().setX(helperCoord.getX() + shiftX);
			node.getCoord().setY(helperCoord.getY() + shiftY);
		}
		
		new NetworkWriter(scenario.getNetwork()).write(targetFile);
		
		
		System.out.println("Network node conversion completed.");		
		System.out.println("========================================");
		

	}

}
