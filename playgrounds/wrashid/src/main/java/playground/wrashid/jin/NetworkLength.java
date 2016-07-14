package playground.wrashid.jin;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;

public class NetworkLength {

	public static void main(String[] args) {
		String networkFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		Network network = GeneralLib.readNetwork(networkFile);
		
//		Coord circleCenter=new CoordImpl(683243.7,247459.2);
//		double radius=700;

		Coord circleCenter= new Coord(682922.588, 247474.957);
		double radius=298;
		
		double networkLength=0;
		for (Link link:network.getLinks().values()){
			if (GeneralLib.getDistance(link.getCoord(), circleCenter)<radius){
				networkLength+=link.getLength()*link.getNumberOfLanes();
			}
		}
		System.out.println("network length:" + networkLength);
		
		
	}
	
}
