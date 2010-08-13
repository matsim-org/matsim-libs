package playground.wrashid.PSF.data.hubCoordinates.hubLinkMapper;

import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class MapLinksToHubs {
	public static void main(String[] args) {
		// key: linkId, value: hub number
		HashMap<Id,Integer> linkHubMapping=new HashMap<Id,Integer>();
		StringMatrix matrix=GeneralLib.readStringMatrix("A:/data/ewz daten/GIS_coordinates_of_managers.txt");
		NetworkLayer network=GeneralLib.readNetwork("A:/data/matsim/input/runRW1003/network-osm-ch.xml.gz");
		
		double averageHubSubManagers=6.381134321058625;
		
		// this is a N^2/expensive operation: browse through all network links for each "HubSubManagers"
	
		for (Link link:network.getLinks().values()){
			for (int i=0;i<matrix.getNumberOfRows();i++){
				Coord coordinateOfCurrentHubSubManager=new CoordImpl(matrix.getDouble(i, 1),matrix.getDouble(i, 2));
				
				if (GeneralLib.getDistance(link.getCoord(), coordinateOfCurrentHubSubManager)<averageHubSubManagers){
					linkHubMapping.put(link.getId(), matrix.convertDoubleToInteger(i, 0));
				}
			}
		}
		
		
		printAllLinkHubMappingsToConsole();
	}
	
	private static int getNumberOfHubs(StringMatrix matrix){
		int numberOfHubs=Integer.MIN_VALUE;
		for (int i=0;i<matrix.getNumberOfRows();i++){
			int currentHubNumber=matrix.convertDoubleToInteger(i, 0);
			if (currentHubNumber>numberOfHubs){
				numberOfHubs=currentHubNumber;
			}
		}
		return numberOfHubs;
	}

	private static void printAllLinkHubMappingsToConsole() {
		// CONTINUE Here.
		
	}
}
