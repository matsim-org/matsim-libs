package playground.mmoyo.input;

import java.util.List;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.api.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Creates the links for the class PTLineAggregator
 * @param net current PT network 
 */
public class PTLinkFactory {
	Network net;
	int intNextId;
	
	public PTLinkFactory(Network net) {
		this.net= net;
		intNextId = getNextLinkId();
	}

	/**
	 * Gets a ptline as a list of nodes and creates directed links (standard) between them
	 */
	public void addNewLinks (List<BasicNode> nodeList){
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

	/**
	 * Creates a standard link for the pt layer, with calculated length
	 */
	private void createPTLink(int intId, BasicNode fromBasicNode, BasicNode toBasicNode, String type){
		Id id =  new IdImpl(intId);
		Link  link = this.net.getFactory().createLink(id, fromBasicNode.getId(), toBasicNode.getId() );
		link.setLength(CoordUtils.calcDistance(fromBasicNode.getCoord(), toBasicNode.getCoord()));
		link.setType(type);
	}
	
	/**
	 * returns the current maximal Id Number of standard links plus one.
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
