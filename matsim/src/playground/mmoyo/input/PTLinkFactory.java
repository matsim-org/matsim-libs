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

/*
 * Creates the links for the class PTLineAggregator 
 */
public class PTLinkFactory {
	NetworkLayer net;
	int intNextId;
	
	public PTLinkFactory(NetworkLayer net) {
		this.net= net;
		intNextId = getNextLinkId();
	}

	/*
	 * Gets a a ptline list of nodes and creates directed links between them
	 */
	public void AddNewLinks (List<BasicNode> nodeList){
		boolean isFirst= true;
		BasicNode lastNode = null;
		
		for (BasicNode basicNode : nodeList){
			if(!isFirst){
				createPTLink(++intNextId, basicNode, lastNode, "Standard");
			}
			isFirst=false;
			lastNode=basicNode;
		}
		lastNode = null;
	}

	/*
	 * Creates Standard links for the pt layer with temporay irrelevant values for freespeed, capacity, numlanes and OrigId
	*/
	private void createPTLink(int intId, BasicNode fromBasicNode, BasicNode toBasicNode, String type){
		Id id =  new IdImpl(intId);
		Link  link = this.net.getFactory().createLink(id, fromBasicNode.getId(), toBasicNode.getId() );
		link.setLength(CoordUtils.calcDistance(fromBasicNode.getCoord(), toBasicNode.getCoord()));
		link.setType(type);
	}
	
	/*
	 * returns the maximal Id Number of standard links plus one
	 */
	public int getNextLinkId(){
		int maxId = -1; 
		int intId;
		for(Link link: this.net.getLinks().values() ){
			if (link.getType().equals("Standard")){
				intId= Integer.parseInt(link.getId().toString());
				if (intId>maxId){
					maxId= intId;
				}
 			}
		}
		return  maxId + 1;
	}
	
}
