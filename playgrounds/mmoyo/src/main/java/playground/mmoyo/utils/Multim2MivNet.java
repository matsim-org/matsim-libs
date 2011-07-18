package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;

/**Removes pt links and pt nodes of a multimodal net*/
public class Multim2MivNet {
	private final static String MIV = "miv_";
	
	private void run (Network net){
		List<Id> removeList = new ArrayList<Id>();
		
		//remove links
		for (Id id : net.getLinks().keySet()){
			if (!id.toString().startsWith(MIV)){
				removeList.add(id);
			}
		}
		for (Id id : removeList){
			net.removeLink(id);
		}
		removeList.clear();
		
		//remove nodes
		for (Id id  : net.getNodes().keySet()){
			if (!id.toString().startsWith(MIV)){
				removeList.add(id);
			}
		}
		for (Id id : removeList){
			net.removeNode(id);
		}
	}
	
	public static void main(String[] args) {
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		DataLoader dataLoader = new DataLoader();
		Network net = dataLoader.readNetwork(netFilePath);
		new Multim2MivNet().run(net);
		
		NetworkWriter popwriter = new NetworkWriter(net);
		popwriter.write(new File(netFilePath).getParent() + "/onlyMIVnet.xml");
	}

}
