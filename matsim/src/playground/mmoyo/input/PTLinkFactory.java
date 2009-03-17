package playground.mmoyo.input;

import java.util.Arrays;
import java.util.List;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordUtils;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.basic.v01.network.BasicNode;

public class PTLinkFactory {
	NetworkLayer net;
	int intNextId;
	
	public PTLinkFactory(NetworkLayer net) {
		this.net= net;
	}

	public void createLinks (List<BasicNode> nodeList){
		int intId= getNextLinkId();
		boolean isFirst= true;
		BasicNode currentNode;
		BasicNode lastNode = null;
		
		for (BasicNode basicNode : nodeList){
			currentNode= basicNode; 
			if(!isFirst){
				/// 11 MARZ  createStandardLink(intId++, currentNode, lastNode, "Standard");
			}
			isFirst=false;
			lastNode=basicNode;
		}
		intNextId= intId+1;
	}
	
	
	private void createStandardLink(int intId, BasicNode fromBasicNode, BasicNode toBasicNode, String type){
		Id id =  new IdImpl(intId);
		Node fromNode = net.getNode(fromBasicNode.getId());
		Node toNode = net.getNode(toBasicNode.getId());
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";
		this.net.createLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type); 
	}
	
	public void addLinksfromNet(NetworkLayer tempNet){
		for (Link l: tempNet.getLinks().values()){
			//11 MARZ createP(l.getId(), l.getFromNode().getId(), l.getToNode().getId().toString(), l.getType());
		}
	}
	
	public int getNextLinkId(){
		int [] intIdArray = new int[net.getLinks().size()];
		int x=0;
		for (Id id : net.getLinks().keySet()){
			int intId =  Integer.parseInt(id.toString());
			intIdArray[x++]=intId;
		}
		Arrays.sort(intIdArray);
		return intIdArray[x-1]+1;
	}
	
}
