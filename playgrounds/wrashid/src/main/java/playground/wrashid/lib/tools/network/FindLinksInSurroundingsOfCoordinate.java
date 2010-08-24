package playground.wrashid.lib.tools.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;


/**
 * @author wrashid
 *
 */
public class FindLinksInSurroundingsOfCoordinate {

	public static void main(String[] args) {
		String inputNetworkPath="A:/data/matsim/input/runRW1003/network-osm-ch.xml.gz";
		Coord coordInFocus=new CoordImpl(683588, 247318);
		double maxDistanceInMeters=200;
		String outputFilePath="A:/temp/surroundingLinks.kml";
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		
		NetworkLayer network= GeneralLib.readNetwork(inputNetworkPath);
		
		for (Link link:network.getLinks().values()){
			if (GeneralLib.getDistance(coordInFocus, link.getCoord())<maxDistanceInMeters){
				basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
			}
		}
		
		basicPointVisualizer.write(outputFilePath);
	}
	
}
