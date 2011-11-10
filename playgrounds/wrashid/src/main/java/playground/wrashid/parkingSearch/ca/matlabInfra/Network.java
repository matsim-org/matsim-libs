package playground.wrashid.parkingSearch.ca.matlabInfra;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class Network {

	public static void main(String[] args) {
		String baseFolder = "H:/data/experiments/TRBAug2011/runs/ktiRun22/output/";
		
		final String networkFileName = baseFolder + "output_network.xml.gz";
		
		NetworkImpl network = GeneralLib.readNetwork(networkFileName);
		
		for (Link link:network.getLinks().values()){
			if (isInsideStudyArea(link.getFromNode().getCoord()) || isInsideStudyArea(link.getToNode().getCoord())){
				//write out the link
				
				// remember the node, so that it can be written out later.
			}
						
		}
		//file 2.
		
		
		
		
		for (Node node:network.getNodes().values()){
			System.out.println(node.getId());
			
		}
		//file 1.
		
		
		
	}
	
	private static boolean isInsideStudyArea(Coord coord){
		Coord studyAreaCenter= ParkingHerbieControler.getCoordinatesQuaiBridgeZH();
		double radiusInMetersOfStudyArea=1000;
		
		return GeneralLib.getDistance(coord, studyAreaCenter)<radiusInMetersOfStudyArea;
	}
	
}
