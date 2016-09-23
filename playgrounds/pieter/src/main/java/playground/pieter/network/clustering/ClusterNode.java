package playground.pieter.network.clustering;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class ClusterNode implements Node {
	private final Node node;

    public ClusterNode(Node n ) {
		this.node = n;
	}
	

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	public final void setOrigId(String id) {
		final String id1 = id;
		NetworkUtils.setOrigId( node, id1 ) ;
	}

	public final void setType(String type) {
		final String type1 = type;
		NetworkUtils.setType(node,type1);
	}

	@Override
	public boolean equals(Object obj) {
		return node.equals(obj);
	}

	@Override
	public final boolean addInLink(Link inlink) {
		return node.addInLink(inlink);
	}

	@Override
	public final boolean addOutLink(Link outlink) {
		return node.addOutLink(outlink);
	}

	public void setCoord(Coord coord) {
		node.setCoord(coord);
	}

	public final void removeInLink(Link inlink) {
		final Link inlink1 = inlink;
		node.removeInLink(inlink1.getId());
	}

	public final void removeOutLink(Link outlink) {
		final Link outlink1 = outlink;
		final Id<Link> outLinkId = outlink1.getId();
		node.removeOutLink(outLinkId);
	}

	public final String getOrigId() {
		return NetworkUtils.getOrigId( node ) ;
	}

	public final String getType() {
		return NetworkUtils.getType( node ) ;
	}

	public final Map<Id<Link>, ? extends Link> getIncidentLinks() {
		return NetworkUtils.getIncidentLinks( node );
	}

	public final Map<Id<Node>, ? extends Node> getInNodes() {
		return NetworkUtils.getInNodes(node);
	}

	public final Map<Id<Node>, ? extends Node> getOutNodes() {
		return NetworkUtils.getOutNodes(node);
	}

	public final Map<Id<Node>, ? extends Node> getIncidentNodes() {
		return NetworkUtils.getIncidentNodes(node);
	}

	@Override
	public Map<Id<Link>, ? extends Link> getInLinks() {
		return node.getInLinks();
	}

	@Override
	public Map<Id<Link>, ? extends Link> getOutLinks() {
		return node.getOutLinks();
	}

	@Override
	public Coord getCoord() {
		return node.getCoord();
	}

	@Override
	public Id<Node> getId() {
		return node.getId();
	}

	@Override
	public String toString() {
		return node.toString();
	}
	

	/**
	 * Once a cluster is chosen by the algorithm, it needs to be set to the new root
	 * @param root
	 */
	public void setNewRoot(NodeCluster root){

	}


	@Override
	public Link removeInLink(Id<Link> linkId) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}


	@Override
	public Link removeOutLink(Id<Link> outLinkId) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}


	@Override
	public Attributes getAttributes() {
		return node.getAttributes();
	}
}
