package playground.mmoyo.input;

import java.util.Arrays;
import java.util.List;
import org.matsim.network.NetworkLayer;
import playground.mmoyo.PTRouter.PTLine;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Node;

public class PTLinkFactory {
	NetworkLayer net;
	int intId;
	
	public PTLinkFactory(NetworkLayer net) {
		this.net= net;
	}

	public void createLinks (PTLine ptLine){
		//this.intId= intId;
		ptLine.getRoute();
		//--> falta en el sentido contrario, quiere decir que hay que hacer esto en ambos sentidos
	}
	
	private void createLinksBetweenNodes(List<String> nodeList){
		boolean first= true;
		String fromNode= "";
		String toNode;
		for (String strNodeId : nodeList){
			toNode= strNodeId;
			if(!first){
				createLink(intId, fromNode, toNode, "Standard");
			}
			first=false;
			fromNode=strNodeId;
		}
		
	}
	
	private void createLink(int intId, String strIdFromNode, String strToNode, String type){
		Id id =  new IdImpl(intId);
		Node fromNode = this.net.getNode(strIdFromNode); 
		Node toNode = this.net.getNode(strToNode);
		double length = fromNode.getCoord().calcDistance(toNode.getCoord());
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";

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
