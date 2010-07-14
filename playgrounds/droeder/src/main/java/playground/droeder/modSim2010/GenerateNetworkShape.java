package playground.droeder.modSim2010;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;

public class GenerateNetworkShape {

	public static void main(String[] args){
		GenerateNetworkShape netShape = new GenerateNetworkShape();
		netShape.run();
	}
	
	public void run(){
		generateNetwork();
	}
	
	Map<Id, Link> links = new HashMap<Id, Link>();
	Map<Id, SortedMap<String, String>> attributes = new HashMap<Id, SortedMap<String, String>>();
	
	private void generateNetwork(){
		Scenario sc = new ScenarioImpl();
		NetworkFactory fac = sc.getNetwork().getFactory();
		Network net = sc.getNetwork();
	}
}
