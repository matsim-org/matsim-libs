package playground.florian;

import java.util.*;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.misc.Time;



public class NetworkToCarNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String NET_FILE="../../inputs/padang_net_v20080618.xml";
		final String OUTPUT_FILE="../../inputs/padang_net_car_v20090604.xml";
		final double freespeedKmPerHour=50; //km/h
		final double capacityVehPerHourPerLane=1500;
		
		double freespeed = freespeedKmPerHour / 3.6;
		//Öffnen des Szenarios
		Scenario sc = new ScenarioImpl();
		Network net = sc.getNetwork();
		new MatsimNetworkReader(net).readFile(NET_FILE);
//		Network net2 = sc.getNetwork();
//		net2.setCapacityPeriod(3600);
		NetworkLayer net3 = new NetworkLayer();
		net3.setCapacityPeriod(1);
		net3.setEffectiveLaneWidth(3.5);
		net3.setEffectiveCellSize(7.5);
		
		// Übertragen der Nodes
		Map<Id,Node> nodes = net.getNodes();
		int j = 0;
		for (Iterator<Id> it = nodes.keySet().iterator(); it.hasNext();){
			j++;
			Id id = it.next();
			Node node = nodes.get(id);
			Coord coord = node.getCoord();
			net3.createNode(id, coord);
		}
		
		//Bearbeiten der Links
		int lanes=0;
		int i = 0;
		int g = 0;
		Map<Id,Link> links = net.getLinks();
		for (Iterator<Id> it = links.keySet().iterator(); it.hasNext();){
			Id id1 = it.next();
			Link link = links.get(id1);
			Id id2 = link.getId();
			Node startNode = net3.getNode(link.getFromNode().getId());
			Node endNode = net3.getNode(link.getToNode().getId());
			double length = link.getLength();
			
			//Bestimmung der Spuranzahl
			double width = link.getCapacity(Time.UNDEFINED_TIME) / (1.33*2);
			if (width < 3.5){
				lanes = 1;
				g++;
			}
			else {
				lanes = (int)(width / 3.5);
			}
			//Berechnung der capacity
			double capacity = capacityVehPerHourPerLane / 3600 * lanes;
			net3.createLink(id2, startNode, endNode, length, freespeed, capacity, lanes);
			i++;
		}
		//Das neue Netzwerk ist fertig --- Erzeuge Ausgabe
		new NetworkWriter(net3,OUTPUT_FILE).write();
		System.out.println("Ausgabe in die Datei " + OUTPUT_FILE + " ist fertig");
		System.out.println("Statistik:");
		System.out.println("Es wurden " + j + " Nodes und " + i + " Links hinzugefügt!");
		System.out.println("Von den " + i + " Links sind " + g + " Links einspurig und " + (i-g) + " Links mehrspurig.");
	}

}
