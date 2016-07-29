package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.LinkedList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * route starts from ToNode of first link and goes to ToNode of last link.
 * 
 * @author wrashid
 *
 */

class Route {

	LinkedList<Link> links=new LinkedList<Link>();
	
	public void addLink(Link link){
		if (links.size()==0 || !links.getLast().equals(link)){
			links.add(link);
		}
	}
	
	public String getNodeString(Network network){
		StringBuffer stringBuffer = new StringBuffer();
		
		for (Link link:links){
			stringBuffer.append(((Link)link).getToNode().getId());
			stringBuffer.append(" ");
		}
		
		return stringBuffer.toString().trim();
	}
	
}
