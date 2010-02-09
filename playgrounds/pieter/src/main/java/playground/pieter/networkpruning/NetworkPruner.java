package playground.pieter.networkpruning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.network.algorithms.NetworkSummary;

public class NetworkPruner {
	private NetworkLayer network;
	private final double minLength = 100;
	private String inFile;
	private String outFile;
	private NetworkCleaner netCleaner = new NetworkCleaner();
	private NetworkSummary netSummary = new NetworkSummary();

	public NetworkPruner(String inFile, String outFile) {
		this.network = new NetworkLayer();
		this.inFile = inFile;
		this.outFile = outFile;
		new MatsimNetworkReader(network).readFile(this.inFile);

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
		new NetworkWriter(this.network,this.outFile).write();
	}

	private void joinOneWayLinks() {
		// TODO Auto-generated method stub
		Map<Id,Node> nodeMap =  network.getNodes();
		Iterator<Node> nodeIterator = nodeMap.values().iterator();
		int linkJoinCount = 0;
		while(nodeIterator.hasNext()){
			 Node currentNode =nodeIterator.next();
			 Map<Id,? extends Link> inLinks = currentNode.getInLinks();
			 Map<Id,? extends Link> outLinks = currentNode.getOutLinks();
			 if(inLinks.size()==1 && outLinks.size()==1){
				 //check that it's not a dead-end, and has same parameters
				 double period = 1;
				 Link inLink = inLinks.values().iterator().next();
				 Link outLink = outLinks.values().iterator().next();
				 Node fromNode = inLink.getFromNode();
				 Node toNode = outLink.getToNode();
				 double inFlow = inLink.getFlowCapacity(period);
				 double outFlow = outLink.getFlowCapacity(period);
				 double inSpeed = inLink.getFreespeed(period);
				 double outSpeed = inLink.getFreespeed(period);
				 int inLanes = inLink.getLanesAsInt(period);
				 int outLanes = outLink.getLanesAsInt(period);
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
