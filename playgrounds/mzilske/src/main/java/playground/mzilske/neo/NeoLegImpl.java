package playground.mzilske.neo;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class NeoLegImpl implements Leg {

	static final String KEY_MODE = "mode";

	final private Node underlyingNode;

	public NeoLegImpl(Node endNode) {
		this.underlyingNode = endNode;
	}

	@Override
	public double getDepartureTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getMode() {
		return (String) underlyingNode.getProperty(KEY_MODE);
	}

	@Override
	public Route getRoute() {
		Relationship r = this.underlyingNode.getSingleRelationship(RelationshipTypes.LEG_TO_ROUTE, Direction.OUTGOING);
		if (r != null) {
			return new NeoNetworkRouteImpl(r.getEndNode());
		} else {
			return null;
		}
	}

	@Override
	public double getTravelTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setDepartureTime(double seconds) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMode(String mode) {
		underlyingNode.setProperty(KEY_MODE, mode);
	}

	@Override
	public void setRoute(Route route) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTravelTime(double seconds) {
		// TODO Auto-generated method stub

	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

}
