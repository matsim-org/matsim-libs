package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;



public class ComposedNode implements Node, Cloneable {


	//Constants

	public static final String SEPARATOR = "-";


	//Enums

	public enum Types {
		EMPTY,
		SOURCE,
		SINK,
		DEAD_END,
		ONE_WAY_PASS,
		TWO_WAY_PASS,
		ONE_WAY_START,
		ONE_WAY_END,
		CROSSING;
	}


	//Attributes

	private Coord coord;

	private final Id id;

	private final Map<Id, ComposedLink> inLinks;

	private final Map<Id, ComposedLink> outLinks;
	
	private final Set<Node> nodes;
	
	private ComposedNode containerNode;
	
	private Types type;

	private List<Link> incidentLinks;


	//Methods
	
	public ComposedNode(Set<Node> nodes) {
		String idText = "";
		for(Node node:nodes)
			idText+=node.getId()+SEPARATOR;
		idText=idText.substring(0, idText.length()-1);
		id = new IdImpl(idText);
		coord = new CoordImpl(0, 0);
		for(Node node:nodes)
			coord.setXY(coord.getX()+node.getCoord().getX(), coord.getY()+node.getCoord().getY());
		coord.setXY(coord.getX()/nodes.size(), coord.getY()/nodes.size());
		this.nodes = nodes;
		inLinks = new HashMap<Id, ComposedLink>();
		outLinks = new HashMap<Id, ComposedLink>();
		for(Node node:nodes) {
			if(node.getClass().equals(ComposedNode.class))
				((ComposedNode)node).setContainerNode(this);
		}
	}

	public List<Link> getIncidentLinks() {
		return incidentLinks;
	}

	public void setIncidentLinks() {
		incidentLinks = new ArrayList<Link>();
		for(Node node:nodes) {
			for(Link link:node.getInLinks().values()) {
				boolean insideLink = false;
				for(Node node2:nodes)
					if(link.getFromNode().getId().equals(node2.getId()))
						insideLink = true;
				if(!insideLink)
					incidentLinks.add(link);
			}
			for(Link link:node.getOutLinks().values()) {
				boolean insideLink = false;
				for(Node node2:nodes)
					if(link.getToNode().getId().equals(node2.getId()))
						insideLink = true;
				if(!insideLink)
					incidentLinks.add(link);
			}			
		}
		for(int i=0; i<incidentLinks.size()-1; i++)
			for(int j=i+1; j<incidentLinks.size(); j++)
				if(((ComposedLink)incidentLinks.get(i)).getAngle()>((ComposedLink)incidentLinks.get(j)).getAngle()) {
					Link temporalLink = incidentLinks.get(i);
					incidentLinks.set(i, incidentLinks.get(j));
					incidentLinks.set(j, temporalLink);
				}
	}
	
	public ComposedNode getContainerNode() {
		return containerNode;
	}


	public void setContainerNode(ComposedNode containerNode) {
		this.containerNode = containerNode;
	}


	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public boolean addInLink(Link link) {
		inLinks.put(link.getId(), (ComposedLink)link);
		return true;
	}

	@Override
	public boolean addOutLink(Link link) {
		outLinks.put(link.getId(), (ComposedLink)link);
		return false;
	}

	@Override
	public Map<Id, ? extends Link> getInLinks() {
		return inLinks;
	}

	@Override
	public Map<Id, ? extends Link> getOutLinks() {
		return outLinks;
	}
	
	public Set<Node> getNodes() {
		return nodes;
	}

	public Types getType() {
		return type;
	}

	public void setType() {
		if(inLinks.size() == 0 && outLinks.size()==0)
			type = Types.EMPTY;
		else if(inLinks.size() == 0)
			type = Types.SOURCE;
		else if(outLinks.size() == 0)
			type = Types.SINK;
		else if(inLinks.size() == 1 && outLinks.size() == 1 && inLinks.values().iterator().next().getFromNode().equals(outLinks.values().iterator().next().getToNode()))
			type = Types.DEAD_END;
		else if(inLinks.size() == 1 && outLinks.size() == 1)
			type = Types.ONE_WAY_PASS;
		else if(inLinks.size() == 2 && outLinks.size() == 1) {
			Iterator<ComposedLink> inLinksIterator = inLinks.values().iterator();
			Link firstInLink = inLinksIterator.next();
			Link secondInLink = inLinksIterator.next();
			if(outLinks.values().iterator().next().getFromNode().equals(firstInLink.getToNode()) || outLinks.values().iterator().next().getFromNode().equals(secondInLink.getToNode()))
				type = Types.ONE_WAY_END;
			else
				type = Types.CROSSING;
		}
		else if(inLinks.size() == 1 && outLinks.size() == 2) {
			Iterator<ComposedLink> outLinksIterator = outLinks.values().iterator();
			Link firstOutLink = outLinksIterator.next();
			Link secondOutLink = outLinksIterator.next();
			if(inLinks.values().iterator().next().getFromNode().equals(firstOutLink.getToNode()) || inLinks.values().iterator().next().getFromNode().equals(secondOutLink.getToNode()))
				type = Types.ONE_WAY_START;
			else
				type = Types.CROSSING;
		}
		else if(inLinks.size() == 2 && outLinks.size() == 2) {
			Iterator<ComposedLink> inLinksIterator = inLinks.values().iterator();
			Link firstInLink = inLinksIterator.next();
			Link secondInLink = inLinksIterator.next();
			Iterator<ComposedLink> outLinksIterator = outLinks.values().iterator();
			Link firstOutLink = outLinksIterator.next();
			Link secondOutLink = outLinksIterator.next();
			if((firstInLink.getFromNode().equals(firstOutLink.getToNode()) && secondInLink.getFromNode().equals(secondOutLink.getToNode())) || (firstInLink.getFromNode().equals(secondOutLink.getToNode()) && secondInLink.getFromNode().equals(firstOutLink.getToNode())))
				type = Types.TWO_WAY_PASS;
			else
				type = Types.CROSSING;
		}
		else
			type = Types.CROSSING;
	}

	public void setType(Types type) {
		this.type = type;
	}
	
	public ComposedNode clone() {
		return new ComposedNode(nodes);
	}

}
