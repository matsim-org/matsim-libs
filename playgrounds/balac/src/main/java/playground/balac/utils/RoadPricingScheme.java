package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class RoadPricingScheme {

	
	public static boolean outside(Node n, Coord coord) {
		
		if (CoordUtils.calcEuclideanDistance(n.getCoord(), coord) > 8000)
		
			return true;
		else 
			return false;
		
	}
	public static boolean inside(Node n, Coord coord) {
		
		
		
		if (CoordUtils.calcEuclideanDistance(n.getCoord(), coord) < 8000)
			
			return true;
		else 
			return false;
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/" + "RoadPricingLinks_8km.txt");
		
		double centerX = 683217.0; 
		double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
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
