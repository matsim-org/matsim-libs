package playground.pieter.networkpruning;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkSummary;

public class NetworkPruner {
	private NetworkLayer network;
	private final double minLength = 100;
	private String inFile;
	private String outFile;
	private NetworkCleaner netCleaner = new NetworkCleaner();
	private NetworkSummary netSummary = new NetworkSummary();

	public NetworkPruner(String inFile, String outFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.inFile = inFile;
		this.outFile = outFile;
		new MatsimNetworkReader(scenario).readFile(this.inFile);

		new NetworkSummary().run(network);
	}


	public void run(){
		this.netCleaner.run(network);
		this.netSummary.run(network);
		pruneIslands();
		joinOneWayLinks();
		System.out.println("  running Network cleaner modules... ");
		this.netCleaner.run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);

		System.out.println("  done.");
		new NetworkWriter(this.network).writeFile(this.outFile);
	}

	private void joinOneWayLinks() {
		Map<Id,NodeImpl> nodeMap =  network.getNodes();
		Iterator<NodeImpl> nodeIterator = nodeMap.values().iterator();
		int linkJoinCount = 0;
		while(nodeIterator.hasNext()){
			 NodeImpl currentNode =nodeIterator.next();
			 Map<Id,? extends Link> inLinks = currentNode.getInLinks();
			 Map<Id,? extends Link> outLinks = currentNode.getOutLinks();
			 if(inLinks.size()==1 && outLinks.size()==1){
				 //check that it's not a dead-end, and has same parameters
				 double period = 1;
				 Link inLink = inLinks.values().iterator().next();
				 Link outLink = outLinks.values().iterator().next();
				 Node fromNode = inLink.getFromNode();
				 Node toNode = outLink.getToNode();
				 double inFlow = inLink.getCapacity(period);
				 double outFlow = outLink.getCapacity(period);
				 double inSpeed = inLink.getFreespeed(period);
				 double outSpeed = inLink.getFreespeed(period);
				 double inLanes = inLink.getNumberOfLanes(period);
				 double outLanes = outLink.getNumberOfLanes(period);
				 double inLength = inLink.getLength();
				 double outLength = outLink.getLength();
				 if((!fromNode.equals(toNode)) &&
					(inFlow == outFlow) &&
					(inSpeed == outSpeed) &&
					(inLanes == outLanes) &&
					(inLength<this.minLength || outLength<this.minLength )){
					 System.out.println("number of links joined: "+ linkJoinCount++);
					 inLink.setToNode(toNode);
					 toNode.addInLink(inLink);
					 currentNode.removeInLink(inLink);
				 }
			 }
//			 Iterator linkIterator<Link> = inLinks.values().iterator();
		}
	}

	private void pruneIslands() {
		// TODO Auto-generated method stub

	}

	public static void main(String args[]){
		String inFile = "./southafrica/network/routes_network.xml";
		String outFile = "./southafrica/network/output_network.xml";
		new NetworkPruner(inFile,outFile).run();

	}
}
