package patryk.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MyNetworkEditor2 {

	public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("networks/network_v09_boende.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:3857");    // EPSG Code 
        
        for(Link link : network.getLinks().values()) {
        	double capacity = link.getCapacity();
        	double freeSpeed = link.getFreespeed();

        	if (freeSpeed > -0.5 && freeSpeed < 0.5)
        		// link.setFreespeed(0);
        		System.out.println("freesp �r 0");
        }
        
        
 //       NetworkWriter writer = new NetworkWriter(network);
  //      writer.write("networks/network_v09.xml");

	}

}
