package playground.mmoyo.input;

import java.util.List;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;
import java.util.Map;

public class PTLinkFactory {
	NetworkLayer net;
	int intNextId;
	
	public PTLinkFactory(NetworkLayer net) {
		this.net= net;
		intNextId = getNextLinkId();
	}

	public void AddNewLinks (List<BasicNode> nodeList){
		boolean isFirst= true;
		BasicNode lastNode = null;
		
		for (BasicNode basicNode : nodeList){
			if(!isFirst){
				createStandardLink(++intNextId, basicNode, lastNode, "Standard");
			}
			isFirst=false;
			lastNode=basicNode;
		}
		lastNode = null;
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
		int maxId=0; 
		for(Map.Entry <Id,Link> entry: this.net.getLinks().entrySet() ){
			if (entry.getValue().getType().equals("Standard")){
				Id id = entry.getKey();
				int intId =  Integer.parseInt(id.toString());
				if (intId > maxId) {maxId=intId;}
			}
		}
		return maxId+1;
	}
	
}
