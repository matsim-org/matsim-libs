package playground.wrashid.oswald;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.lib.GeneralLib;


public class GenerateTabSeparatedNetwork {

	public static void main(String[] args) {
		
		Network network = GeneralLib.readNetwork("C:/tmp/network.xml");
		
		System.out.println("linkId\tfromNode\ttoNode\tfreeSpeed\tcapacity\tnumberOfLanes");
		
		for (Link link:network.getLinks().values()){
			if (isInStudyArea(link.getCoord())){
				System.out.print(link.getId());
				System.out.print("\t");
				System.out.print(link.getFromNode().getId());
				System.out.print("\t");
				System.out.print(link.getToNode().getId());
				System.out.print("\t");
				System.out.print(link.getFreespeed());
				System.out.print("\t");
				System.out.print(link.getCapacity()/network.getCapacityPeriod()*3600);
				System.out.print("\t");
				System.out.print(link.getNumberOfLanes());
				System.out.println();
			}
			
		}
		
		System.out.println("nodeId\tx\ty");
		
		for (Node node:network.getNodes().values()){
			if (isInStudyArea(node.getCoord())){
				System.out.print(node.getId());
				System.out.print("\t");
				System.out.print(node.getCoord().getX());
				System.out.print("\t");
				System.out.print(node.getCoord().getY());
				System.out.println();
			}
		}
		
	}

	private static boolean isInStudyArea(Coord coord) {
		if (coord.getX() > 671225 && coord.getX() < 694675) {
			if (coord.getY() > 236580 && coord.getY() < 259310) {
				return true;
			}
		}
		return false;
	}

}
