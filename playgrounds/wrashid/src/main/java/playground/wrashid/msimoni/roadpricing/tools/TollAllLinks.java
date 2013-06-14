package playground.wrashid.msimoni.roadpricing.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;

public class TollAllLinks {

	public static void main(String[] args) {
		Network network = GeneralLib.readNetwork("P:/Projekte/matsim/data/switzerland/networks/ivtch/network.xml");
		for (Id id:network.getLinks().keySet()){
			System.out.println("<link id=\"" + id + "\" />");
		}
	}
	
}
