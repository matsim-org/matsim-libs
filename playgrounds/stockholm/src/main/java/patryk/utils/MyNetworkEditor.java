package patryk.utils;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MyNetworkEditor {

	public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        ArrayList<Link> linksToRemove = new ArrayList<>();
        ArrayList<Node> affectedNodes = new ArrayList<>();
        
        config.network().setInputFile("networks/network_v09.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:3857");    // EPSG Code 
        
        for(Link link : network.getLinks().values()) {
        	double freeSpeed = link.getFreespeed();
        	double capacity = link.getCapacity();
        	Node fromNode = link.getFromNode();
        	Node toNode = link.getToNode();
        	
        	if (freeSpeed > 24.9 || capacity < 1) {
        		linksToRemove.add(link);
        		affectedNodes.add(fromNode);
        		affectedNodes.add(toNode);
        		System.out.println("link removed");
        	}
        }
        
        for(int i = 0; i < linksToRemove.size() - 1; i++) {
        	network.removeLink(linksToRemove.get(i).getId());
        }
        
        NetworkWriter writer = new NetworkWriter(network);
        writer.write("networks/network_v09_boende.xml");

	}

}
