package playground.pieter.networkpruning;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkSummary;
import org.matsim.core.utils.misc.Time;

public class ShortLinkFuser {
	private NetworkLayer network;
	private double minLength;
	private String inFile;
	private String outFile;
	private final int numberOfIterations = 20;
	private double shortestLink = 1000;
	private int oneWayLinkJointCount;
	private int twoWayLinkJointCount;

	public ShortLinkFuser(String inFile, String outFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.inFile = inFile;
		this.outFile = outFile;
		new MatsimNetworkReader(scenario).readFile(this.inFile);
		new NetworkSummary().run(network);
		this.minLength = 1000;
	}

	public ShortLinkFuser(String inFile, String outFile, double minLength) {
		ScenarioImpl scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.inFile = inFile;
		this.outFile = outFile;
		new MatsimNetworkReader(scenario).readFile(this.inFile);
		new NetworkSummary().run(network);
		this.minLength = minLength;
	}
	
	public void run(){
		new NetworkCleaner().run(network);
		for (int i=0; i<this.numberOfIterations; i++){
			System.out.println("Iteration number " + i);
			joinOneWayLinks();
			joinTwoWayLinks();
			new NetworkCleaner().run(network);
			new NetworkMergeDoubleLinks().run(network);
			findShortestLink();
		}
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);

		System.out.println("Pruning  done. Shortest link is " + this.shortestLink);
		System.out.println("One way links removed: " + this.oneWayLinkJointCount);
		System.out.println("Two way links removed: " + this.twoWayLinkJointCount);
		new NetworkWriter(this.network).writeFile(this.outFile);
		System.out.println("File written to "+ this.outFile);
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
				 double time = Time.UNDEFINED_TIME;
				 Link inLink = inLinks.values().iterator().next();
				 Link outLink = outLinks.values().iterator().next();
				 Node fromNode = inLink.getFromNode();
				 Node toNode = outLink.getToNode();
				 double inFlow = inLink.getCapacity(time);
				 double outFlow = outLink.getCapacity(time);
				 double inSpeed = inLink.getFreespeed(time);
				 double outSpeed = outLink.getFreespeed(time);
				 double inLanes = inLink.getNumberOfLanes(time);
				 double outLanes = outLink.getNumberOfLanes(time);
				 double inLength = inLink.getLength();
				 double outLength = outLink.getLength();
				 if((!fromNode.equals(toNode)) &&
					(inFlow == outFlow) &&
					(inSpeed == outSpeed) &&
					(inLanes == outLanes) &&
					((inLength<this.minLength) || (outLength<this.minLength))
					){

					 inLink.setToNode(toNode);
					 toNode.addInLink(inLink);
					 currentNode.removeInLink(inLink);
					 inLink.setLength(inLink.getLength() + outLink.getLength());
					 linkJoinCount++;
					 this.oneWayLinkJointCount++;
				 }
			 }
		}
		System.out.println("number of oneway links joined: "+ linkJoinCount);
	}
	private void joinTwoWayLinks() {
		Map<Id,NodeImpl> nodeMap =  network.getNodes();
		Iterator<NodeImpl> nodeIterator = nodeMap.values().iterator();
		int linkJoinCount = 0;
		while(nodeIterator.hasNext()){
			 NodeImpl currentNode =nodeIterator.next();
			 Map<Id,? extends Link> inLinks = currentNode.getInLinks();
			 Map<Id,? extends Link> outLinks = currentNode.getOutLinks();
			 if(inLinks.size()==2 && outLinks.size()==2){
				 //check that it's not a dead-end, and has same parameters

				 double period = 1;
				 Iterator<? extends Link> inLinkIterator = inLinks.values().iterator();
				 Iterator<? extends Link> outLinkIterator = outLinks.values().iterator();
				 Link inLink1 = inLinkIterator.next();
				 Link inLink2 = inLinkIterator.next();

//				 operating from this scheme:
//				 fromNode			inLink1			currentNode			outLink1			toNode
//				 *-------------------------------------->*--------------------------------------->*
//				  <-------------------------------------- <---------------------------------------
//				 		            outLink2							inLink2
//				 so first need to establish which outLink is which
				 Link outLinkX = outLinkIterator.next();
				 Link outLinkY = outLinkIterator.next();
				 Link outLink2 = null;
				 Link outLink1 = null;
				 if(outLinkX.getToNode().equals(inLink1.getFromNode())){
					 outLink2 = outLinkX;
					 outLink1 = outLinkY;
//					 this means the other two have to have share the same to/from node
					 if(!outLink1.getToNode().equals(inLink2.getFromNode())){
//						 something's wrong, move on
						 continue;
					 }
				 }else if(outLinkY.getToNode().equals(inLink1.getFromNode())){
					 outLink2 = outLinkY;
					 outLink1 = outLinkX;
//					 this means the other two have to have share the same to/from node
					 if(!outLink1.getToNode().equals(inLink2.getFromNode())){
//						 something's wrong, move on
						 continue;
					 }
				 }else{
//					 something else is wrong, move on
					 continue;
				 }

				 Node fromNode = inLink1.getFromNode();
				 Node toNode = inLink2.getFromNode();
				 
//				 final sanity check:
				 if(
						 (toNode.equals(fromNode) ||
						 toNode.equals(currentNode) ||
						 fromNode.equals(currentNode))
				 ) {
//					 System.out.println("Something funny with Node " + currentNode.getId());
					 continue;
				 }


				 double inFlow = inLink1.getCapacity(period);
				 double outFlow = outLink1.getCapacity(period);

				 double inSpeed = inLink1.getFreespeed(period);
				 double outSpeed = outLink1.getFreespeed(period);

				 double inLanes = inLink1.getNumberOfLanes(period);
				 double outLanes = outLink1.getNumberOfLanes(period);

				 double inLength1 = inLink1.getLength();
				 double inLength2 = inLink2.getLength();
				 double outLength1 = outLink1.getLength();
				 double outLength2 = outLink2.getLength();

				 if(
					(inFlow == outFlow) &&
					(inSpeed == outSpeed) &&
					(inLanes == outLanes) &&
					(inLength1<this.minLength || outLength1<this.minLength ) /*&&
					(inLength1 == outLength2 && inLength2 == outLength1)*/
					){

					 inLink1.setToNode(toNode);
					 toNode.addInLink(inLink1);
					 inLink1.setLength(inLength1 + outLength1);
					 inLink2.setToNode(fromNode);
					 inLink2.setLength(inLength2 + outLength2);
					 fromNode.addInLink(inLink2);
					 currentNode.removeInLink(inLink1);
					 currentNode.removeInLink(inLink2);
					 linkJoinCount++;
					 this.twoWayLinkJointCount++;
					 
				 }
//				 System.out.println("number of two-way links joined: "+ linkJoinCount);
			 }
		}
		System.out.println("number of two-way links joined: "+ linkJoinCount);

	}


	private void findShortestLink(){
		Iterator<? extends LinkImpl> linkIterator = this.network.getLinks().values().iterator();
		while (linkIterator.hasNext()){
			double length = linkIterator.next().getLength();
			if (length<this.shortestLink){
				this.shortestLink = length;
			}
		}
	}
	public static void main(String args[]){
		String inFile = "./southafrica/network/routes_networkRAW.xml";
		String outFile = "./southafrica/network/routes_network_50m+.xml";
		new ShortLinkFuser(inFile,outFile).run();

	}
}
