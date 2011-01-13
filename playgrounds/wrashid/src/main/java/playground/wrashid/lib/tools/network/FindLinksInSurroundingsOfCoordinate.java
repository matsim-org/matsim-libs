package playground.wrashid.lib.tools.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
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
		String inputNetworkPath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/output_network.xml.gz";
		Coord coordInFocus=new CoordImpl(683702, 247854);
		double maxDistanceInMeters=1000;
		String outputFilePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/analysis/surroundingLinks-17560001856956FT.kml";
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		
		NetworkImpl network= GeneralLib.readNetwork(inputNetworkPath);
		
		for (Link link:network.getLinks().values()){
			if (GeneralLib.getDistance(coordInFocus, link.getCoord())<maxDistanceInMeters){
				basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
			}
		}
		
		basicPointVisualizer.write(outputFilePath);
	}
	
}
