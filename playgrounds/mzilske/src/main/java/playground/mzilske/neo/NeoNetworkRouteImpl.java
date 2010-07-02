package playground.mzilske.neo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;

public class NeoNetworkRouteImpl implements NetworkRoute {

	static final String KEY_DESCRIPTION = "description";

	private Node underlyingNode;

	public NeoNetworkRouteImpl(Node endNode) {
		this.underlyingNode = endNode;
	}

	@Override
	public List<Id> getLinkIds() {
		System.out.println(getRouteDescription());
		List<Id> ids = new ArrayList<Id>();
		for (Node link : traverseLinks()) {
			Id id = new NeoLinkImpl(link).getId();
			ids.add(id);
		}
		return ids;
	}

	private Traverser traverseLinks() {
		final List<String> nodes = Arrays.asList(getRouteDescription().split("[ \t\n]+"));
		Node startLink = getStartLink().getUnderlyingNode();
		Traverser t = startLink.traverse(
				Order.BREADTH_FIRST, 
				new StopEvaluator() {

					@Override
					public boolean isStopNode(TraversalPosition arg0) {
						if (arg0.depth() % 2 == 0) {
							return false; // is a link
						} else {
							int nNode = arg0.depth() / 2;
							if (nNode == nodes.size())
								return true;
							NeoNodeImpl node = new NeoNodeImpl(arg0.currentNode());
							if (!nodes.get(nNode).equals(node.getId().toString())) {
								return true;
							} else {
								return false;
							}
						}
					}
					
				},
				new ReturnableEvaluator() {
					
					@Override
					public boolean isReturnableNode(TraversalPosition arg0) {
						if (arg0.currentNode().hasProperty(NeoLinkImpl.KEY_ID)) {
							return true;
						} else {
							return false;
						}
					}
				},
				RelationshipTypes.LINK_TO, 
				Direction.OUTGOING);
		return t;
	}

	private String getRouteDescription() {
		return (String) underlyingNode.getProperty(KEY_DESCRIPTION);
	}

	@Override
	public NetworkRoute getSubRoute(Id fromLinkId, Id toLinkId) {
		throw new RuntimeException();
	}

	@Override
	public double getTravelCost() {
		throw new RuntimeException();
	}

	@Override
	public Id getVehicleId() {
		throw new RuntimeException();
	}

	@Override
	public void setLinkIds(Id startLinkId, List<Id> linkIds, Id endLinkId) {
		throw new RuntimeException();
	}

	@Override
	public void setTravelCost(double travelCost) {
		throw new RuntimeException();
	}

	@Override
	public void setVehicleId(Id vehicleId) {
		throw new RuntimeException();
	}

	@Override
	public void setEndLinkId(Id linkId) {
		throw new RuntimeException();
	}

	@Override
	public void setStartLinkId(Id linkId) {
		throw new RuntimeException();
	}

	@Override
	public double getDistance() {
		throw new RuntimeException();
	}

	@Override
	public Id getEndLinkId() {
		Link endLink = new NeoLinkImpl(underlyingNode.getSingleRelationship(RelationshipTypes.END_LINK, Direction.OUTGOING).getEndNode());
		return endLink.getId();
	}

	@Override
	public Id getStartLinkId() {
		Link startLink = getStartLink();
		return startLink.getId();
	}

	private NeoLinkImpl getStartLink() {
		NeoLinkImpl startLink = new NeoLinkImpl(underlyingNode.getSingleRelationship(RelationshipTypes.START_LINK, Direction.OUTGOING).getEndNode());
		return startLink;
	}

	@Override
	public double getTravelTime() {
		throw new RuntimeException();
	}

	@Override
	public void setDistance(double distance) {
		throw new RuntimeException();
	}

	@Override
	public void setTravelTime(double travelTime) {
		throw new RuntimeException();
	}

	@Override
	public NetworkRoute clone() {
		throw new RuntimeException();
	}

	public void unroll() {
		//		if (route instanceof NetworkRoute) {
		//			BasicNetworkRoute networkRoute = (BasicNetworkRoute) route;
		//			int i = 0;
		//			long startlinkid = index.getSingleNode(NeoLinkImpl.KEY_ID, route.getStartLinkId());
		//			properties.clear();
		//			properties.put("i", i++);
		//			inserter.createRelationship(routeId, startlinkid, RelationshipTypes.ON_ROUTE, properties);
		//			String[] parts = networkRoute.routeDescription.split("[ \t\n]+");
		//			long previous = 0;
		//			for (String nodeId : parts) {
		//				long nodeid = index.getSingleNode(NeoNodeImpl.KEY_ID, nodeId);
		//				if (previous != 0) {
		//					long linkid = findLink(previous, nodeid);
		//					properties.clear();
		//					properties.put("i", i++);
		//					inserter.createRelationship(routeId, linkid, RelationshipTypes.ON_ROUTE, properties);
		//				}
		//				previous = nodeid;
		//			}
		//			long endlinkid = index.getSingleNode(NeoLinkImpl.KEY_ID, route.getEndLinkId());
		//			properties.clear();
		//			properties.put("i", i++);
		//			inserter.createRelationship(routeId, endlinkid, RelationshipTypes.ON_ROUTE, properties);
		//		}
	}

	//	private long findLink(long fromnodeid, long tonodeid) {
	//		for (SimpleRelationship r : inserter.getRelationships(fromnodeid)) {
	//			if (r.getType().name().equals(RelationshipTypes.LINK_TO.name()) && r.getStartNode() == fromnodeid) {
	//				long linkid = r.getEndNode();
	//				for (SimpleRelationship rr : inserter.getRelationships(linkid)) {
	//					if (rr.getType().name().equals(RelationshipTypes.LINK_TO.name()) && rr.getStartNode() == linkid) {
	//						if (rr.getEndNode() == tonodeid) {
	//							return linkid;
	//						}
	//					}
	//				}
	//			}
	//		}
	//		throw new RuntimeException();
	//	}

}
