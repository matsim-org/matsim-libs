package playground.mmoyo.input;

import java.util.Arrays;
import java.util.List;
import org.matsim.network.NetworkLayer;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.basic.v01.BasicNode;

public class PTLinkFactory {
	NetworkLayer net;
	int intNextId;
	
	public PTLinkFactory(NetworkLayer net) {
		this.net= net;
		intNextId=getNextLinkId();
	}

	public void createLinks (List<BasicNode> nodeList){
		int intId= intNextId;
		boolean isFirst= true;
		BasicNode currentNode;
		BasicNode lastNode = null;
		
		for (BasicNode basicNode : nodeList){
			currentNode= basicNode; 
			if(!isFirst){
				createStandardLink(intId++, currentNode, lastNode);
				//createStandardLink(intSecondId++, lastNode, currentNode);
			}
			isFirst=false;
			lastNode=basicNode;
		}
		intNextId= intId+1;
	}
	
	private void createStandardLink(int intId, BasicNode fromBasicNode, BasicNode toBasicNode){
		Id id =  new IdImpl(intId);
		Node fromNode = net.getNode(fromBasicNode.getId());
		Node toNode = net.getNode(toBasicNode.getId());
		System.out.println(fromBasicNode.getId());
		double length = fromNode.getCoord().calcDistance(toNode.getCoord());
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";
		String type= "Standard";
		this.net.createLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type); 
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
