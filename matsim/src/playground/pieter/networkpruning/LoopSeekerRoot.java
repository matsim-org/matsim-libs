package playground.pieter.networkpruning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public class LoopSeekerRoot extends LoopSeekerNode{

	ArrayList<LinkImpl> inWelds;
	ArrayList<LinkImpl> outWelds;
	
	public ArrayList<NodeImpl> ringNodes;

	protected double loopLength;
	protected double streetCap;
	protected double capPeriod;
//	private Map<Id, PathTreeNode> descendantNodes;
	public boolean loopFound;
	
//	constructor
	public LoopSeekerRoot(NodeImpl rootNode, double loopLength, double streetCap, double capPeriod){
		super();
		this.ringNodes = new ArrayList<NodeImpl>();
		this.inWelds = new ArrayList<LinkImpl>();
		this.outWelds = new ArrayList<LinkImpl>();
		this.capPeriod = capPeriod;
		this.streetCap=streetCap;
		this.loopLength = loopLength;
		this.setPointsToNode(rootNode);
		this.rootNode = this;
		this.isRoot = true;
		assignLinksToFields();
		makeOffspring();
	}

	public void weldLinks(NodeImpl centroidNode) {
		// welds inlinks and outlinks to said Node
		Iterator<LinkImpl> inLinkIt = inWelds.iterator();
		Iterator<LinkImpl> outLinkIt = outWelds.iterator();
		while(inLinkIt.hasNext()){
			LinkImpl currentInLink = inLinkIt.next();
			currentInLink.getFromNode().removeInLink(currentInLink);
			currentInLink.setToNode(centroidNode);
		}
		while(outLinkIt.hasNext()){
			LinkImpl currentOutLink = outLinkIt.next();
			currentOutLink.getFromNode().removeOutLink(currentOutLink);
			currentOutLink.setFromNode(centroidNode);
		}
		
	}
	
	

}
