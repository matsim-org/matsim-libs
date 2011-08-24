package playground.wrashid.lib.tools.network;

import java.util.LinkedList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;

public class PrintFreeSpeeds {

	public static void main(String[] args) {
		String inputNetworkPath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run2/output_network.xml.gz";
		
		NetworkImpl network = GeneralLib.readNetwork(inputNetworkPath);
		
		LinkedList<Double> list=new LinkedList<Double>();
		for (Link link:network.getLinks().values()){
			double freespeed = link.getFreespeed();
			if (!list.contains(freespeed)){
				list.add(freespeed);
				System.out.println(freespeed);
			}
		}
	}
	
}
