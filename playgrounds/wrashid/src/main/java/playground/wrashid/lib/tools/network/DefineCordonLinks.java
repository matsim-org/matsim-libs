package playground.wrashid.lib.tools.network;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;

public class DefineCordonLinks {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputNetworkPath="\\\\kosrae.ethz.ch\\ivt-home\\wrashid\\data\\cvs\\ivt\\studies\\switzerland\\networks\\teleatlas-ivtcheu\\network.xml.gz";
		Coord center = new Coord(682548.0, 247525.5);
		double radiusInMeters = 1500;
		
		NetworkImpl network = (NetworkImpl) GeneralLib.readNetwork(inputNetworkPath);
		
		LinkedList<Link> incomingLinks=new LinkedList<Link>();
		LinkedList<Link> outgoingLinks=new LinkedList<Link>();
		
		for (Link link:network.getLinks().values()){
			if (GeneralLib.getDistance(center, link.getFromNode().getCoord())>radiusInMeters && GeneralLib.getDistance(center, link.getToNode().getCoord())<radiusInMeters){
				incomingLinks.add(link);
			}
			
			if (GeneralLib.getDistance(center, link.getFromNode().getCoord())<radiusInMeters && GeneralLib.getDistance(center, link.getToNode().getCoord())>radiusInMeters){
				outgoingLinks.add(link);
			}
		}
		
		System.out.println("incoming links:");
		
		for (Link link:incomingLinks){
			System.out.println("<link id=\"" + link.getId() + "\" />");
		}
		
		System.out.println("outgoing links:");
		
		for (Link link:outgoingLinks){
			System.out.println("<link id=\"" + link.getId() + "\" />");
		}
		
		

	}

}
