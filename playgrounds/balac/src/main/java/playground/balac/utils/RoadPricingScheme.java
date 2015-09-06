package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class RoadPricingScheme {

	
	public static boolean outside(Node n, Coord coord) {
		
		if (CoordUtils.calcDistance(n.getCoord(), coord) > 4000)
		
			return true;
		else 
			return false;
		
	}
	public static boolean inside(Node n, Coord coord) {
		
		
		
		if (CoordUtils.calcDistance(n.getCoord(), coord) < 4000)
			
			return true;
		else 
			return false;
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/" + "RoadPricingLinks.txt");
		
		double centerX = 683217.0; 
		double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		
		for(Link l : scenario.getNetwork().getLinks().values()) {
			
			if (RoadPricingScheme.outside(l.getFromNode(), coord) && RoadPricingScheme.inside(l.getToNode(), coord)) {
				
				outLink.write("<link id=\"");
				outLink.write(l.getId().toString() + "\" />");
				//outLink.write(l.getId().toString() + " ");
				outLink.newLine();
				
			}
			
		}
		outLink.flush();
		outLink.close();
		
		

	}

}
