package playground.wrashid.thelma.y2030.psl;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class TransitAgentAnalyis {

	public static void main(String[] args) {
		Matrix parkingTimesEC = GeneralLib
				.readStringMatrix("A:/for marina/26. april 2012/parkingTimesAndEnergyConsumptionCH.txt");
		Network network = GeneralLib.readNetwork("H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz");

		HashMap<String, Id> agentIds = new HashMap();

		BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();

		for (int i = 0; i < parkingTimesEC.getNumberOfRows(); i++) {
			String actType = parkingTimesEC.getString(i, 4);
			Id<Link> linkId=Id.create(parkingTimesEC.getString(i, 3), Link.class);
			Link link=network.getLinks().get(linkId);
			if (actType.equalsIgnoreCase("tta")) {
				agentIds.put(parkingTimesEC.getString(i, 0), null);
				// System.out.println("x");
				basicPointVisualizer.addPointCoordinate(new CoordImpl(link.getCoord()), "", Color.GREEN);
			}
		}

		// for (String agentId:agentIds.keySet()){
		// System.out.println(agentId);
		// }

		basicPointVisualizer.write("c:/temp/abdd.kml");
	}

}
