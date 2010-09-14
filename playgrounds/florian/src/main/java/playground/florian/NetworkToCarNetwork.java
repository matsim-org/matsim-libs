package playground.florian;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;



public class NetworkToCarNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		final String NET_FILE="../../inputs/networks/padang_net_v20080618.xml";
		final String OUTPUT_FILE="../../inputs/networks/padang_net_car_v20090604.xml";
		final double freespeedKmPerHour=50; //km/h
		final double capacityVehPerHourPerLane=1500;

		double freespeed = freespeedKmPerHour / 3.6;


		HashSet<String> excludes = getExludes();

		//oeffnen des Szenarios
		ScenarioImpl sc = new ScenarioImpl();
		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(NET_FILE);
//		Network net2 = sc.getNetwork();
//		net2.setCapacityPeriod(3600);
		NetworkImpl net3 = NetworkImpl.createNetwork();
		net3.setCapacityPeriod(1);
		net3.setEffectiveLaneWidth(3.5);
		net3.setEffectiveCellSize(7.5);

		// uebertragen der Nodes
		Map<Id,Node> nodes = net.getNodes();
		int j = 0;
		for (Iterator<Id> it = nodes.keySet().iterator(); it.hasNext();){
			j++;
			Id id = it.next();
			Node node = nodes.get(id);
			Coord coord = node.getCoord();
			net3.createAndAddNode(id, coord);
		}

		//Bearbeiten der Links
		int lanes=0;
		int i = 0;
		int g = 0;
		Map<Id,Link> links = net.getLinks();
		for (Iterator<Id> it = links.keySet().iterator(); it.hasNext();){
			Id id1 = it.next();
			if (excludes.contains(id1.toString())) {
				continue;
			}
			Link link = links.get(id1);
			Id id2 = link.getId();
			Node startNode = net3.getNodes().get(link.getFromNode().getId());
			Node endNode = net3.getNodes().get(link.getToNode().getId());
			double length = link.getLength();

			//Bestimmung der Spuranzahl
			double width = link.getCapacity() / (1.33*2);
			if (width < 3.5){
				lanes = 1;
				g++;
			}
			else {
				lanes = (int)(width / 3.5);
			}
			//Berechnung der capacity
			double capacity = (capacityVehPerHourPerLane / 3600) * lanes;
			net3.createAndAddLink(id2, startNode, endNode, length, freespeed, capacity, lanes);
			i++;
		}

		new NetworkCleaner().run(net3);
		//Das neue Netzwerk ist fertig --- Erzeuge Ausgabe
		new NetworkWriter(net3).write(OUTPUT_FILE);
		System.out.println("Ausgabe in die Datei " + OUTPUT_FILE + " ist fertig");
		System.out.println("Statistik:");
		System.out.println("Es wurden " + j + " Nodes und " + i + " Links hinzugefügt!");
		System.out.println("Von den " + i + " Links sind " + g + " Links einspurig und " + (i-g) + " Links mehrspurig.");
	}

	public static HashSet<String> getExludes() {
		HashSet<Integer> list = new HashSet<Integer>();
		list.add(9718);
		list.add(9227);
		list.add(5636);
		list.add(8498);
		list.add(146);
		list.add(7263);
		list.add(11095);
		list.add(9511);
		list.add(10431);
		list.add(12041);
		list.add(8338);
		list.add(9894);
		list.add(10480);
		list.add(1354);
		list.add(11702);
		list.add(1074);
		list.add(6873);
		list.add(4851);
		list.add(9457);
		list.add(1571);
		list.add(9352);
		list.add(10956);
		list.add(202);
		list.add(3056);
		list.add(4628);
		list.add(5822);
		list.add(10068);
		list.add(7155);
		list.add(9137);
		list.add(9082);
		list.add(1386);
		list.add(10621);
		list.add(11305);
		list.add(10455);
		list.add(5227);
		list.add(2999);
		list.add(859);
		list.add(3040);
		list.add(2502);
		list.add(6342);
		list.add(11625);
		list.add(1611);
		list.add(5507);
		list.add(2636);
		list.add(11910);
		list.add(177);
		list.add(954);
		list.add(3446);
		list.add(11752);
		list.add(6231);
		list.add(11765);
		list.add(10421);
		list.add(2749);
		list.add(2188);
		list.add(4097);
		list.add(4458);
		list.add(9391);
		HashSet<Integer> list2 = new HashSet<Integer>();
		for (Integer i : list) {
			list2.add(i+100000);
		}
		list.addAll(list2);
		HashSet<String> ret = new HashSet<String>();
		for (Integer i : list) {
			ret.add(i.toString());
		}
		return ret;
	}

}
