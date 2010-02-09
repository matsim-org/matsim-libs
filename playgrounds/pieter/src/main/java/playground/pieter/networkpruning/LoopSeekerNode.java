package playground.pieter.networkpruning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;

public class LoopSeekerNode {
	protected Node pointsToNode;
	protected boolean isRoot;
	protected boolean hasParent;
	protected boolean hasKids;
	protected double distanceToRoot;
	protected double distanceToParent;
	protected LoopSeekerNode parentNode;
	protected LoopSeekerRoot rootNode;
	protected Link linkFromParent;
	protected Link linkToParent;
	protected ArrayList<Link> outLinks;
	protected ArrayList<Link> inLinks;
	protected ArrayList<Node> possibleOffSpring;
	protected ArrayList<LoopSeekerNode> childNodes;
//	protected Map<Id, PathTreeNode> descendantNodes;
	
//	constructors
	public LoopSeekerNode(){
		this.outLinks = new ArrayList<Link>();
		this.inLinks = new ArrayList<Link>();
		this.possibleOffSpring = new ArrayList<Node>();
		this.childNodes = new ArrayList<LoopSeekerNode>();
	}
	public LoopSeekerNode(Node childNode, LoopSeekerNode parentNode){
		this.isRoot = false;
		this.setPointsToNode(childNode);
		this.parentNode = parentNode;
		this.outLinks = new ArrayList<Link>();
		this.inLinks = new ArrayList<Link>();
		this.possibleOffSpring = new ArrayList<Node>();
		this.childNodes = new ArrayList<LoopSeekerNode>();
		assignLinksToFields();
		this.distanceToParent = linkFromParent.getLength();
		this.hasParent = true;
		if(this.parentNode.isRoot){
			this.distanceToRoot = this.distanceToParent;
			this.rootNode = (LoopSeekerRoot) this.parentNode;
		}else{
			this.distanceToRoot = this.distanceToParent + this.parentNode.distanceToRoot;
			this.rootNode = this.parentNode.rootNode;
		}
		if(!this.rootNode.loopFound && this.distanceToRoot < this.rootNode.loopLength){
			makeOffspring();
			checkForRing();
		}
	
	}

	private void checkForRing() {
		// TODO Auto-generated method stub
		Iterator<LoopSeekerNode> nodeIt = this.childNodes.iterator();
		while(nodeIt.hasNext()){
			LoopSeekerNode currentNode = nodeIt.next();
			if(currentNode.pointsToNode.equals(rootNode.pointsToNode)&&currentNode.distanceToRoot<rootNode.loopLength){
				traceBackToRoot(currentNode);
				rootNode.loopFound = true;
			}
		}
	}
	private void traceBackToRoot(LoopSeekerNode currentNode) {
		// TODO Auto-generated method stub
		while(!currentNode.isRoot){
			rootNode.ringNodes.add(currentNode.pointsToNode);
			rootNode.inWelds.addAll(currentNode.inLinks);
			rootNode.outWelds.addAll(currentNode.outLinks);
			currentNode = currentNode.parentNode;
		}
	}
	public void addChild(Node childNode){
		this.childNodes.add(new LoopSeekerNode(childNode,this));
	}
	
	protected void assignLinksToFields() {
		// assigns linkToParent, outLinks, inLinks
		if(!this.isRoot){
			Map<Id, ? extends Link> outMap = this.pointsToNode.getOutLinks();
			Iterator<? extends Link> linkIterator = outMap.values().iterator();
			while (linkIterator.hasNext()){
				Link currentLink = linkIterator.next();
				if (currentLink.getToNode().equals(this.parentNode.getPointsToNode())) 
					this.linkToParent = currentLink;
				else{
						this.outLinks.add(currentLink);
						this.possibleOffSpring.add(currentLink.getToNode());
					}
			}
		}else{
			Map<Id, ? extends Link> outMap = this.pointsToNode.getOutLinks();
			Iterator<? extends Link> linkIterator = outMap.values().iterator();
			while (linkIterator.hasNext()){
				Link currentLink = linkIterator.next();

					this.outLinks.add(currentLink);
					this.possibleOffSpring.add(currentLink.getToNode());

			}
		}
		//inLinks
		if(!this.isRoot){
			Map<Id, ? extends Link> inMap = this.pointsToNode.getInLinks();
			Iterator<? extends Link> linkIterator = inMap.values().iterator();
			while (linkIterator.hasNext()){
				Link currentLink = linkIterator.next();
				if (currentLink.getFromNode().equals(this.parentNode.getPointsToNode())) 
					this.linkFromParent = currentLink;
				else
					this.inLinks.add(currentLink);

			}
		}else{
			Map<Id, ? extends Link> inMap = this.pointsToNode.getInLinks();
			Iterator<? extends Link> linkIterator = inMap.values().iterator();
			while (linkIterator.hasNext()){
				Link currentLink = linkIterator.next();

					this.inLinks.add(currentLink);

			}
		}
	}



	public ArrayList<Link> getOutLinks() {
		return outLinks;
	}

	protected void makeOffspring() {
		// TODO Auto-generated method stub
		Iterator<Node> nodeIt = this.possibleOffSpring.iterator();
		while (nodeIt.hasNext()){
			Node currentNode = nodeIt.next();
			if(nodeQualifies(currentNode)){
				addChild(currentNode);
			}
		}
	}

	public Node getPointsToNode() {
		return pointsToNode;
	}

	public void setPointsToNode(Node pointsToNode) {
		this.pointsToNode = pointsToNode;
	}

	public void findRing(double maxLength, double streetCap) {
		// TODO Auto-generated method stub

	}

	private boolean nodeQualifies(Node currentNode) {
		// TODO Auto-generated method stub
		boolean isStreetNode = false;
		boolean isThruNode = false;
//		first check that it has at least two inLinks and 2 outLinks, and that they are of street capacity
		Map<Id, ? extends Link> inLinkMap = currentNode.getInLinks();
		Map<Id, ? extends Link> outLinkMap = currentNode.getOutLinks();
		if(inLinkMap.size() >= 2 && outLinkMap.size() >= 2)
			isThruNode = true;
		isStreetNode = checkIfStreetNode(inLinkMap);
		isStreetNode = checkIfStreetNode(outLinkMap);
		if(isThruNode && isStreetNode)
			return true;
		return false;			
		
	}



	private boolean checkIfStreetNode(Map<Id, ? extends Link> linkMap) {
		// TODO Auto-generated method stub
		Iterator<? extends Link> linkIterator = linkMap.values().iterator();
		while(linkIterator.hasNext()){
			if(linkIterator.next().getCapacity(this.rootNode.capPeriod) != this.rootNode.streetCap)
				return false;
		}
		return true;
	}

}
