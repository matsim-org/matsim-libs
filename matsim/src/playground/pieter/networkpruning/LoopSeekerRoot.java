package playground.pieter.networkpruning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;

public class LoopSeekerRoot extends LoopSeekerNode{

	ArrayList<Link> inWelds;
	ArrayList<Link> outWelds;
	
	public ArrayList<Node> ringNodes;

	protected double loopLength;
	protected double streetCap;
	protected double capPeriod;
//	private Map<Id, PathTreeNode> descendantNodes;
	public boolean loopFound;
	
//	constructor
	public LoopSeekerRoot(Node rootNode, double loopLength, double streetCap, double capPeriod){
		super();
		this.ringNodes = new ArrayList<Node>();
		this.inWelds = new ArrayList<Link>();
		this.outWelds = new ArrayList<Link>();
		this.capPeriod = capPeriod;
		this.streetCap=streetCap;
		this.loopLength = loopLength;
		this.setPointsToNode(rootNode);
		this.rootNode = this;
		this.isRoot = true;
		assignLinksToFields();
		makeOffspring();
	}

	public void weldLinks(Node centroidNode) {
		// welds inlinks and outlinks to said Node
		Iterator<Link> inLinkIt = inWelds.iterator();
		Iterator<Link> outLinkIt = outWelds.iterator();
		while(inLinkIt.hasNext()){
			Link currentInLink = inLinkIt.next();
			currentInLink.getFromNode().removeInLink(currentInLink);
			currentInLink.setToNode(centroidNode);
		}
		while(outLinkIt.hasNext()){
			Link currentOutLink = outLinkIt.next();
			currentOutLink.getFromNode().removeOutLink(currentOutLink);
			currentOutLink.setFromNode(centroidNode);
		}
		
	}
	
	

}
